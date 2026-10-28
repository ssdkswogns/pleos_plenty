package app.plenty.ui.garden

import androidx.compose.runtime.Immutable
import app.plenty.core.GardenLayout
import app.plenty.core.content.MaterialIds
import app.plenty.data.GardenSnapshot
import app.plenty.data.ItemIds
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
 * The app only shows game screens while parked (the platform covers them while driving), so the
 * badge is "parked" until real charging data is connected (T08).
 */
fun GardenSnapshot.toGardenUiState(): GardenUiState {
    val bySlot = plants.associateBy { it.slot }
    return GardenUiState(
        pots = (1..GardenLayout.SLOT_COUNT).map { slot ->
            bySlot[slot]?.let { PotUi(it.state.id, PlantArt.of(it.state.species, it.state.stage)) }
        },
        totalDistanceKm = totalDistanceKm,
        materials = MaterialCounts(
            moonDew = count(ItemIds.material(MaterialIds.MOON_DEW)),
            cloudLight = count(ItemIds.material(MaterialIds.CLOUD_LIGHT)),
            windStone = count(ItemIds.material(MaterialIds.WIND_STONE)),
        ),
        vehicle = VehicleBadge.Parked,
    )
}
