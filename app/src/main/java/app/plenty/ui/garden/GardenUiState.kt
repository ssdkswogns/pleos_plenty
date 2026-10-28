package app.plenty.ui.garden

import androidx.compose.runtime.Immutable
import app.plenty.core.GardenLayout
import app.plenty.ui.art.PlantArt

/** Everything the garden home (03) draws. Screens read this; they never grant rewards themselves. */
@Immutable
data class GardenUiState(
    /** Index = slot id - 1; always [GardenLayout.SLOT_COUNT] entries, null = empty pot. */
    val pots: List<PotUi?>,
    /** Sum of settled journey distance. Null = not known yet, which must never be shown as 0 km. */
    val totalDistanceKm: Double?,
    val materials: MaterialCounts,
    val vehicle: VehicleBadge,
) {
    val filledPotCount: Int get() = pots.count { it != null }
}

@Immutable
data class PotUi(val plantId: Long, val art: PlantArt)

@Immutable
data class MaterialCounts(val moonDew: Int, val cloudLight: Int, val windStone: Int)

sealed interface VehicleBadge {
    data object Parked : VehicleBadge

    /** [percent] only when the vehicle reported a valid state of charge. */
    data class Charging(val percent: Int?) : VehicleBadge
}

/**
 * Layout-only data copied from the 03-home mockup until the repository exists (T05).
 * These are not game-balance values (HANDOFF §2).
 */
val MockGardenState = GardenUiState(
    pots = listOf(
        PotUi(plantId = 1, art = PlantArt.MoonbellMature),
        PotUi(plantId = 2, art = PlantArt.CloudFernMature),
        PotUi(plantId = 3, art = PlantArt.TideLotusMature),
    ) + List(GardenLayout.SLOT_COUNT - 3) { null },
    totalDistanceKm = 128.4,
    materials = MaterialCounts(moonDew = 12, cloudLight = 8, windStone = 4),
    vehicle = VehicleBadge.Charging(percent = 64),
)
