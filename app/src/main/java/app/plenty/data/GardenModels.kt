package app.plenty.data

import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialId
import app.plenty.core.content.RegionId
import app.plenty.core.content.SpeciesId
import app.plenty.core.content.TraitId
import app.plenty.core.rules.PlantState

/** A plant as the UI sees it: growth state plus its pot. */
data class PottedPlant(val state: PlantState, val slot: Int)

/** Live garden contents for the home screen. */
data class GardenSnapshot(
    val plants: List<PottedPlant>,
    val inventory: Map<String, Int>,
    /** Sum of settled journeys; 0 before the first journey, which is a known value. */
    val totalDistanceKm: Double,
) {
    fun count(itemId: String): Int = inventory[itemId] ?: 0
}

/** Result of settling a journey, identical every time it is asked for again. */
sealed interface SettlementView {
    data class Settled(
        val journeyId: Long,
        val distanceKm: Double,
        val growth: List<GrowthRecord>,
        val material: MaterialId?,
        val materialCount: Int,
        val seed: SpeciesId?,
        /** True when this call found an existing result and granted nothing. */
        val alreadySettled: Boolean,
    ) : SettlementView

    /** Odometer not readable yet; the journey stays open. */
    data object Pending : SettlementView

    data class Rejected(val journeyId: Long) : SettlementView
}

data class GrowthRecord(
    val plantId: Long,
    val xpGained: Int,
    val stageBefore: GrowthStage,
    val stageAfter: GrowthStage,
)

/** What 08/09/13 show for one journey, read back from storage (never recomputed). */
data class JourneyResult(
    val journeyId: Long,
    val state: JourneyResultState,
    val source: JourneySource,
    val startedAt: Long,
    /** Valid distance; null unless [state] is [JourneyResultState.Settled]. */
    val distanceKm: Double?,
    /** Null = arrival region unknown (or outside the supported regions until T10 tells them apart). */
    val arrivalRegion: RegionId?,
    val growth: List<GrowthResult>,
    val material: MaterialId?,
    val materialCount: Int,
    val seed: SpeciesId?,
)

enum class JourneyResultState {
    /** Still driving, or not yet parked long enough. */
    Underway,

    /** Parked but the end odometer was unreadable; settlement is retried for the same journey. */
    CheckingDistance,
    Settled,
    Rejected,
}

/** One companion's stored growth. The xp/prefix snapshots are null for journeys settled before v2. */
data class GrowthResult(
    val plantId: Long,
    val species: SpeciesId,
    val xpGained: Int,
    val stageBefore: GrowthStage,
    val stageAfter: GrowthStage,
    val xpBefore: Int?,
    val xpAfter: Int?,
    val prefixAfter: TraitId?,
    val traitPoints: Int,
) {
    val grew: Boolean get() = stageAfter != stageBefore
}

/** A row of the journey log (여정 탭). */
data class JourneyLogEntry(
    val journeyId: Long,
    val state: JourneyResultState,
    val startedAt: Long,
    val distanceKm: Double?,
    val arrivalRegion: RegionId?,
    val companionCount: Int,
)

object ItemIds {
    fun mallang(species: SpeciesId) = "mallang:${species.value}"
    fun material(material: MaterialId) = "material:${material.value}"
}

/** `waterside=5;city=3` <-> map. Plain text keeps the schema simple and readable in a DB browser. */
internal object TraitPointsCodec {
    fun encode(points: Map<TraitId, Int>): String =
        points.entries.joinToString(";") { "${it.key.value}=${it.value}" }

    fun decode(text: String): Map<TraitId, Int> =
        if (text.isEmpty()) emptyMap() else text.split(';').associate { entry ->
            val (key, value) = entry.split('=')
            TraitId(key) to value.toInt()
        }
}

internal fun PlantEntity.toState() = PlantState(
    id = id,
    species = SpeciesId(species),
    stage = GrowthStage.valueOf(stage),
    xp = xp,
    xpRemainder = xpRemainder,
    stageTraitPoints = TraitPointsCodec.decode(stageTraitPoints),
    sproutPrefix = sproutPrefix?.let(::TraitId),
    maturePrefix = maturePrefix?.let(::TraitId),
)

internal fun PlantEntity.withState(state: PlantState) = copy(
    stage = state.stage.name,
    xp = state.xp,
    xpRemainder = state.xpRemainder,
    stageTraitPoints = TraitPointsCodec.encode(state.stageTraitPoints),
    sproutPrefix = state.sproutPrefix?.value,
    maturePrefix = state.maturePrefix?.value,
)

internal fun List<Long>.encodeIds() = joinToString(",")

internal fun String.decodeIds(): List<Long> = if (isEmpty()) emptyList() else split(',').map(String::toLong)

internal fun JourneyEntity.resultState() = when {
    status == JourneyStatus.SETTLED -> JourneyResultState.Settled
    status == JourneyStatus.REJECTED -> JourneyResultState.Rejected
    arrivedAt != null -> JourneyResultState.CheckingDistance
    else -> JourneyResultState.Underway
}

internal fun JourneyEntity.toLogEntry() = JourneyLogEntry(
    journeyId = id,
    state = resultState(),
    startedAt = startedAt,
    distanceKm = distanceKm.takeIf { status == JourneyStatus.SETTLED },
    arrivalRegion = arrivalRegion?.let(::RegionId),
    companionCount = companionIds.decodeIds().size,
)

internal fun JourneyEntity.toResult(growth: List<GrowthWithSpecies>) = JourneyResult(
    journeyId = id,
    state = resultState(),
    source = JourneySource.valueOf(source),
    startedAt = startedAt,
    distanceKm = distanceKm.takeIf { status == JourneyStatus.SETTLED },
    arrivalRegion = arrivalRegion?.let(::RegionId),
    growth = growth.map { row ->
        val g = row.growth
        GrowthResult(
            plantId = g.plantId,
            species = SpeciesId(row.species),
            xpGained = g.xpGained,
            stageBefore = GrowthStage.valueOf(g.stageBefore),
            stageAfter = GrowthStage.valueOf(g.stageAfter),
            xpBefore = g.xpBefore,
            xpAfter = g.xpAfter,
            prefixAfter = g.prefixAfter?.let(::TraitId),
            traitPoints = g.traitPoints,
        )
    },
    material = rewardMaterial?.let(::MaterialId),
    materialCount = rewardMaterialCount,
    seed = rewardSeed?.let(::SpeciesId),
)
