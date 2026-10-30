package app.plenty.data

import androidx.room.withTransaction
import app.plenty.core.GardenLayout
import app.plenty.core.content.DefaultContent
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialId
import app.plenty.core.content.RegionId
import app.plenty.core.content.SpeciesId
import app.plenty.core.content.SpeciesIds
import app.plenty.core.content.TraitId
import app.plenty.core.rules.GrowthEngine
import app.plenty.core.rules.PlantGrowth
import app.plenty.core.rules.SettlementInput
import app.plenty.core.rules.SettlementPlan
import app.plenty.core.rules.SettlementPlanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import kotlin.math.min
import kotlin.random.Random

/**
 * Every grant follows "check ledger -> compute -> write state + ledger" inside one transaction
 * (plan §데이터 모델). The ledger key is unique, so repeating a settlement, a crash mid-way or a
 * restart can never pay twice; a repeated call returns the stored result instead.
 */
class GardenRepository(
    private val db: PlentyDatabase,
    private val content: GameContent = DefaultContent,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val random: Random = Random.Default,
) {
    private val dao = db.gardenDao()
    private val planner = SettlementPlanner(content)
    private val growthEngine = GrowthEngine(content)

    val garden: Flow<GardenSnapshot> = combine(
        dao.observePottedPlants(),
        dao.observeInventory(),
        dao.observeTotalDistanceKm(),
    ) { plants, inventory, distance ->
        GardenSnapshot(
            plants = plants.map { PottedPlant(it.toState(), slot = checkNotNull(it.slot)) },
            inventory = inventory.associate { it.itemId to it.quantity },
            totalDistanceKm = distance,
        )
    }

    /** Companions saved on 05 for the next departure; empty when nothing is ready. */
    val readyCompanions: Flow<List<Long>> =
        dao.observeReadySelection().map { it?.companionIds?.decodeIds().orEmpty() }

    /** Newest journey whose result has not been shown (settled, rejected or checking distance). */
    val unseenResultId: Flow<Long?> = dao.observeUnseenResultId()

    val journal: Flow<List<JourneyLogEntry>> = dao.observeJournal().map { rows -> rows.map { it.toLogEntry() } }

    fun journeyResult(journeyId: Long): Flow<JourneyResult?> =
        combine(dao.observeJourney(journeyId), dao.observeGrowthWithSpecies(journeyId)) { journey, growth ->
            journey?.toResult(growth)
        }

    /** The first-launch 말랑 (04 empty garden). Granted once per install, even across restarts. */
    suspend fun ensureStarterGift() = db.withTransaction {
        if (dao.hasLedger(STARTER_KEY)) return@withTransaction
        addItem(ItemIds.mallang(STARTER_SPECIES), 1)
        dao.insertLedger(RewardLedgerEntity(STARTER_KEY, journeyId = null, content.rulesVersion, clock.millis()))
    }

    /** Moves one 말랑 of [species] from the bag into an empty pot. No material cost (GAME_RULES.md §1). */
    suspend fun plantMallang(species: SpeciesId, slot: Int): Long = db.withTransaction {
        require(slot in 1..GardenLayout.SLOT_COUNT) { "slot $slot out of range" }
        check(dao.plantInSlot(slot) == null) { "slot $slot is occupied" }
        takeItem(ItemIds.mallang(species), 1)
        dao.insertPlant(
            PlantEntity(
                species = species.value,
                stage = GrowthStage.SEED.name,
                xp = 0,
                xpRemainder = 0.0,
                stageTraitPoints = "",
                sproutPrefix = null,
                maturePrefix = null,
                slot = slot,
                createdAt = clock.millis(),
            ),
        )
    }

    /**
     * Saves 05's choice for the next departure (INTERACTIONS.md §05/06). No journey is created and
     * no odometer is read: that happens only when the vehicle actually leaves park ([depart]).
     */
    suspend fun prepareJourney(companionIds: List<Long>) = db.withTransaction {
        requireCompanions(companionIds)
        check(dao.plants(companionIds).count { it.slot != null } == companionIds.size) { "unknown companion" }
        dao.upsertReadySelection(ReadySelectionEntity(companionIds = companionIds.encodeIds(), readyAt = clock.millis()))
    }

    /** Fixes companions and the start odometer. Only one journey can be open at a time. */
    suspend fun startJourney(companionIds: List<Long>, startOdometerKm: Double?, source: JourneySource): Long =
        db.withTransaction {
            requireCompanions(companionIds)
            check(dao.activeJourney() == null) { "a journey is already open" }
            check(dao.plants(companionIds).size == companionIds.size) { "unknown companion" }
            insertActiveJourney(companionIds, startOdometerKm, source)
        }

    /**
     * The vehicle left park with [startOdometerKm] read at that moment. An open journey continues
     * unchanged (its companions were fixed at its own departure); otherwise the ready selection
     * becomes an ACTIVE journey and is cleared. Returns the open journey, or null if nothing was ready.
     */
    suspend fun depart(startOdometerKm: Double, source: JourneySource): Long? = db.withTransaction {
        dao.activeJourney()?.let { open ->
            if (open.arrivedAt != null) dao.updateJourney(open.copy(arrivedAt = null))
            return@withTransaction open.id
        }
        val ready = dao.readySelection() ?: return@withTransaction null
        dao.deleteReadySelection()
        val chosen = ready.companionIds.decodeIds()
        val existing = dao.plants(chosen).mapTo(HashSet()) { it.id }
        val companions = chosen.filter { it in existing }
        if (companions.isEmpty()) null else insertActiveJourney(companions, startOdometerKm, source)
    }

    /** Records that 08 has been shown, so the growth and discovery sequence plays only once. */
    suspend fun markResultSeen(journeyId: Long) = dao.markResultSeen(journeyId, clock.millis())

    suspend fun activeJourneyId(): Long? = dao.activeJourney()?.id

    /**
     * Settles a journey exactly once. Asking again returns the stored result with
     * [SettlementView.Settled.alreadySettled] = true and changes nothing.
     */
    suspend fun settleJourney(journeyId: Long, endOdometerKm: Double?, arrivalRegion: RegionId?): SettlementView =
        db.withTransaction {
            val journey = checkNotNull(dao.journey(journeyId)) { "no journey $journeyId" }
            val rewardKey = journeyKey(journeyId)
            if (journey.status != JourneyStatus.ACTIVE || dao.hasLedger(rewardKey)) {
                return@withTransaction storedView(journey)
            }
            val day = LocalDate.now(clock).toString()
            val companions = journey.companionIds.decodeIds()
            val plants = dao.plants(companions).associateBy { it.id }
            val plan = planner.plan(
                SettlementInput(
                    startOdometerKm = journey.startOdometerKm,
                    endOdometerKm = endOdometerKm,
                    companionIds = companions,
                    arrivalRegion = arrivalRegion,
                    plants = plants.mapValues { it.value.toState() },
                    xpGrantedToday = dao.dailyXp(day, companions).associate { it.plantId to it.xp },
                    regionRewardedToday = arrivalRegion != null && dao.regionClaimedOn(arrivalRegion.value, day),
                    regionVisitedBefore = arrivalRegion != null && dao.regionEverClaimed(arrivalRegion.value),
                ),
                random,
            )
            when (plan) {
                SettlementPlan.Pending -> {
                    // Parked but unreadable: 08 shows "거리 확인 중" for this same journey.
                    if (journey.arrivedAt == null) dao.updateJourney(journey.copy(arrivedAt = clock.millis()))
                    SettlementView.Pending
                }
                is SettlementPlan.Reject -> {
                    dao.updateJourney(
                        journey.copy(
                            status = JourneyStatus.REJECTED,
                            endOdometerKm = endOdometerKm,
                            arrivalRegion = arrivalRegion?.value,
                            settledAt = clock.millis(),
                            rulesVersion = content.rulesVersion,
                        ),
                    )
                    dao.insertLedger(RewardLedgerEntity(rewardKey, journeyId, content.rulesVersion, clock.millis()))
                    SettlementView.Rejected(journeyId)
                }
                is SettlementPlan.Settle -> {
                    dao.updatePlants(plan.growth.map { plants.getValue(it.after.id).withState(it.after) })
                    val granted = dao.dailyXp(day, companions).associate { it.plantId to it.xp }
                    dao.upsertDailyXp(
                        plan.growth.map { DailyXpEntity(day, it.after.id, (granted[it.after.id] ?: 0) + it.xpGained) },
                    )
                    val trait = arrivalRegion?.let(content::region)?.trait
                    dao.insertGrowth(plan.growth.map { it.toEntity(journeyId, trait) })
                    val reward = plan.regionReward
                    if (reward != null) {
                        addItem(ItemIds.material(reward.material), reward.materialCount)
                        reward.seed?.let { addItem(ItemIds.mallang(it), 1) }
                        dao.insertRegionClaim(RegionClaimEntity(reward.region.value, day, journeyId))
                    }
                    val settled = journey.copy(
                        status = JourneyStatus.SETTLED,
                        endOdometerKm = endOdometerKm,
                        arrivalRegion = arrivalRegion?.value,
                        settledAt = clock.millis(),
                        distanceKm = plan.distanceKm,
                        rewardMaterial = reward?.material?.value,
                        rewardMaterialCount = reward?.materialCount ?: 0,
                        rewardSeed = reward?.seed?.value,
                        rulesVersion = content.rulesVersion,
                    )
                    dao.updateJourney(settled)
                    dao.insertLedger(RewardLedgerEntity(rewardKey, journeyId, content.rulesVersion, clock.millis()))
                    settledView(settled, alreadySettled = false)
                }
            }
        }

    /** Spends materials to add trait points to a seed or sprout (GAME_RULES.md §3). */
    suspend fun applyMaterial(plantId: Long, material: MaterialId, count: Int) = db.withTransaction {
        val plant = checkNotNull(dao.plant(plantId)) { "no plant $plantId" }
        val boosted = growthEngine.applyMaterial(plant.toState(), material, count)
        takeItem(ItemIds.material(material), count)
        dao.updatePlants(listOf(plant.withState(boosted)))
    }

    private fun requireCompanions(companionIds: List<Long>) {
        require(companionIds.isNotEmpty() && companionIds.size <= GardenLayout.MAX_COMPANIONS) {
            "1..${GardenLayout.MAX_COMPANIONS} companions required"
        }
        require(companionIds.distinct().size == companionIds.size) { "duplicate companions" }
    }

    private suspend fun insertActiveJourney(companionIds: List<Long>, startOdometerKm: Double?, source: JourneySource) =
        dao.insertJourney(
            JourneyEntity(
                status = JourneyStatus.ACTIVE,
                source = source.name,
                companionIds = companionIds.encodeIds(),
                startOdometerKm = startOdometerKm,
                endOdometerKm = null,
                arrivalRegion = null,
                startedAt = clock.millis(),
                settledAt = null,
                distanceKm = null,
                rewardMaterial = null,
                rewardMaterialCount = 0,
                rewardSeed = null,
                rulesVersion = null,
            ),
        )

    /** Trait points only accrue below the mature threshold (GrowthEngine.gainXp). */
    private fun PlantGrowth.toEntity(journeyId: Long, trait: TraitId?) = JourneyGrowthEntity(
        journeyId = journeyId,
        plantId = after.id,
        xpGained = xpGained,
        stageBefore = before.stage.name,
        stageAfter = after.stage.name,
        xpBefore = before.xp,
        xpAfter = after.xp,
        prefixAfter = when (after.stage) {
            GrowthStage.SEED -> null
            GrowthStage.SPROUT -> after.sproutPrefix?.value
            GrowthStage.MATURE -> after.maturePrefix?.value
        },
        traitPoints = if (trait == null) 0 else min(xpGained, (content.growth.matureXp - before.xp).coerceAtLeast(0)),
    )

    private suspend fun storedView(journey: JourneyEntity): SettlementView =
        if (journey.status == JourneyStatus.REJECTED) {
            SettlementView.Rejected(journey.id)
        } else {
            settledView(journey, alreadySettled = true)
        }

    private suspend fun settledView(journey: JourneyEntity, alreadySettled: Boolean): SettlementView.Settled =
        SettlementView.Settled(
            journeyId = journey.id,
            distanceKm = journey.distanceKm ?: 0.0,
            growth = dao.growthFor(journey.id).map {
                GrowthRecord(it.plantId, it.xpGained, GrowthStage.valueOf(it.stageBefore), GrowthStage.valueOf(it.stageAfter))
            },
            material = journey.rewardMaterial?.let(::MaterialId),
            materialCount = journey.rewardMaterialCount,
            seed = journey.rewardSeed?.let(::SpeciesId),
            alreadySettled = alreadySettled,
        )

    private suspend fun addItem(itemId: String, amount: Int) {
        dao.upsertInventory(InventoryEntity(itemId, (dao.quantity(itemId) ?: 0) + amount))
    }

    private suspend fun takeItem(itemId: String, amount: Int) {
        val owned = dao.quantity(itemId) ?: 0
        check(owned >= amount) { "not enough $itemId: need $amount, have $owned" }
        dao.upsertInventory(InventoryEntity(itemId, owned - amount))
    }

    companion object {
        const val STARTER_KEY = "starter:v1"
        val STARTER_SPECIES = SpeciesIds.MOONBELL

        fun journeyKey(journeyId: Long) = "journey:$journeyId"
    }
}
