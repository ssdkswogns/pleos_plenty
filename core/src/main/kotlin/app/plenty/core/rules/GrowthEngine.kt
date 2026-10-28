package app.plenty.core.rules

import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialId
import app.plenty.core.content.SpeciesId
import app.plenty.core.content.TraitId
import kotlin.math.floor
import kotlin.math.min

/** Growth-relevant state of one potted plant. Persistence maps to and from this (T05). */
data class PlantState(
    val id: Long,
    val species: SpeciesId,
    val stage: GrowthStage = GrowthStage.SEED,
    val xp: Int = 0,
    /** Fractional XP carried to the next journey, 0 <= remainder < 1. */
    val xpRemainder: Double = 0.0,
    /** Trait points gathered during the current stage; they decide the next stage's name prefix. */
    val stageTraitPoints: Map<TraitId, Int> = emptyMap(),
    /** Null = no regional trait was known while it was a seed, so the name has no prefix. */
    val sproutPrefix: TraitId? = null,
    val maturePrefix: TraitId? = null,
)

/** Companions are fixed when the journey starts; changing them later does not change who is paid. */
data class JourneyInput(
    val distance: JourneyDistance,
    val companionIds: List<Long>,
    /** Trait of the arrival region; null when the location was unknown or stale. */
    val arrivalTrait: TraitId?,
)

data class PlantGrowth(val before: PlantState, val after: PlantState, val xpGained: Int) {
    val reachedStages: List<GrowthStage>
        get() = GrowthStage.entries.filter { it > before.stage && it <= after.stage }
}

sealed interface SettlementOutcome {
    data class Settled(val growth: List<PlantGrowth>) : SettlementOutcome

    /** Distance not known yet: nothing is granted, settle again later. */
    data object Pending : SettlementOutcome

    /** Distance is invalid: nothing is granted and the caller re-baselines the odometer. */
    data class Rejected(val reason: JourneyDistance.Reason) : SettlementOutcome
}

/**
 * Pure growth rules (GAME_RULES.md §1-3). Never touches storage: callers persist the returned
 * states together with the reward ledger in one transaction.
 */
class GrowthEngine(private val content: GameContent) {

    private val config = content.growth

    fun settle(
        journey: JourneyInput,
        plants: Map<Long, PlantState>,
        xpGrantedToday: Map<Long, Int>,
    ): SettlementOutcome {
        val companions = journey.companionIds.distinct()
        require(companions.size <= config.maxCompanions) {
            "at most ${config.maxCompanions} companions, got ${companions.size}"
        }
        val km = when (val distance = journey.distance) {
            is JourneyDistance.Valid -> distance.km
            JourneyDistance.Unknown -> return SettlementOutcome.Pending
            is JourneyDistance.Invalid -> return SettlementOutcome.Rejected(distance.reason)
        }
        val growth = companions.mapNotNull { id ->
            val plant = plants[id] ?: return@mapNotNull null
            grow(plant, km, journey.arrivalTrait, granted = xpGrantedToday[id] ?: 0)
        }
        return SettlementOutcome.Settled(growth)
    }

    /** Adds trait points from [count] materials. Mature plants are rejected: their name is final. */
    fun applyMaterial(plant: PlantState, material: MaterialId, count: Int): PlantState {
        require(count > 0) { "count must be positive" }
        require(plant.stage != GrowthStage.MATURE) { "mature plant names are final" }
        val trait = requireNotNull(content.traitBoostedBy(material)) { "no trait is boosted by $material" }
        return plant.copy(
            stageTraitPoints = plant.stageTraitPoints.plusPoints(trait.id, count * config.traitPointsPerMaterial),
        )
    }

    /** Highest-scoring trait; ties go to the earlier trait in the content list. Null if nothing scored. */
    fun prefixFor(points: Map<TraitId, Int>): TraitId? {
        var best: TraitId? = null
        var bestScore = 0
        for (trait in content.traits) {
            val score = points[trait.id] ?: 0
            if (score > bestScore) {
                best = trait.id
                bestScore = score
            }
        }
        return best
    }

    private fun grow(plant: PlantState, km: Double, trait: TraitId?, granted: Int): PlantGrowth {
        val raw = km * config.xpPerKm + plant.xpRemainder
        var xp = floor(raw).toInt()
        var remainder = raw - xp
        val room = (config.dailyXpCapPerPlant - granted).coerceAtLeast(0)
        if (xp > room || room == 0) {
            // Capped: overflow and this journey's fraction are dropped (GAME_RULES.md §2).
            xp = room
            remainder = 0.0
        }
        val after = gainXp(plant, xp, trait).copy(xpRemainder = remainder)
        return PlantGrowth(before = plant, after = after, xpGained = xp)
    }

    /** Splits XP across stage thresholds so each stage only scores the XP earned inside it. */
    private fun gainXp(start: PlantState, amount: Int, trait: TraitId?): PlantState {
        var plant = start
        var left = amount
        while (left > 0) {
            val threshold = nextThreshold(plant.stage)
            val step = if (threshold == null) left else min(left, (threshold - plant.xp).coerceAtLeast(0))
            plant = plant.copy(
                xp = plant.xp + step,
                stageTraitPoints = if (trait != null && threshold != null) {
                    plant.stageTraitPoints.plusPoints(trait, step)
                } else {
                    plant.stageTraitPoints
                },
            )
            left -= step
            if (threshold != null && plant.xp >= threshold) plant = advance(plant)
        }
        return plant
    }

    private fun nextThreshold(stage: GrowthStage): Int? = when (stage) {
        GrowthStage.SEED -> config.sproutXp
        GrowthStage.SPROUT -> config.matureXp
        GrowthStage.MATURE -> null
    }

    private fun advance(plant: PlantState): PlantState {
        val prefix = prefixFor(plant.stageTraitPoints)
        return when (plant.stage) {
            GrowthStage.SEED -> plant.copy(stage = GrowthStage.SPROUT, sproutPrefix = prefix, stageTraitPoints = emptyMap())
            GrowthStage.SPROUT -> plant.copy(stage = GrowthStage.MATURE, maturePrefix = prefix, stageTraitPoints = emptyMap())
            GrowthStage.MATURE -> plant
        }
    }

    private fun Map<TraitId, Int>.plusPoints(trait: TraitId, points: Int): Map<TraitId, Int> =
        if (points == 0) this else this + (trait to (this[trait] ?: 0) + points)
}
