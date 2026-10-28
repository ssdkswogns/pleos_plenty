package app.plenty.data

import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialId
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
