package app.plenty.ui.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.GardenLayout
import app.plenty.ui.art.TravelerCart
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.PotSlot
import app.plenty.ui.components.StatusChip
import app.plenty.ui.theme.PlentyRadius
import app.plenty.ui.theme.PlentyTheme
import java.util.Locale

private val PanelShape = RoundedCornerShape(PlentyRadius.panel)

/** How far the cart's wheels tuck under the pots panel, as in 03-home (cart bottom 708, panel top 674). */
private val CartOverlap = 34.dp
private const val CART_ASPECT = 1586f / 992f

/** 03 정원 홈: hero copy, the traveler cart with planted pots, and the 6-slot panel. */
@Composable
fun GardenScreen(
    state: GardenUiState,
    onPotClick: (slotNumber: Int) -> Unit,
    onEmptyPotClick: () -> Unit,
    onPrepareJourney: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            GardenHero(
                totalDistanceKm = state.totalDistanceKm,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 48.dp),
            )
            // Only the artwork scales with the window; text and buttons keep their size (HANDOFF §5).
            val cartWidth = min(maxWidth * 0.64f, (maxHeight + CartOverlap) * CART_ASPECT)
            TravelerCart(
                pots = state.pots.map { it?.art },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 17.dp)
                    .offset(y = CartOverlap)
                    .width(cartWidth),
            )
        }
        PotsPanel(
            state = state,
            onPotClick = onPotClick,
            onEmptyPotClick = onEmptyPotClick,
            onPrepareJourney = onPrepareJourney,
        )
    }
}

/** Header capsules for the garden: vehicle status, then the three material counts. */
@Composable
fun RowScope.GardenStatusChips(state: GardenUiState) {
    when (val vehicle = state.vehicle) {
        is VehicleBadge.Charging -> StatusChip(
            icon = R.drawable.ic_bolt,
            text = vehicle.percent?.let { stringResource(R.string.status_charging_percent, it) }
                ?: stringResource(R.string.status_charging),
        )
        VehicleBadge.Parked -> StatusChip(icon = R.drawable.ic_check, text = stringResource(R.string.status_parked))
    }
    MaterialChip(R.drawable.ic_drop, R.string.material_moon_dew, state.materials.moonDew)
    MaterialChip(R.drawable.ic_spark, R.string.material_cloud_light, state.materials.cloudLight)
    MaterialChip(R.drawable.ic_stone, R.string.material_wind_stone, state.materials.windStone)
}

@Composable
private fun MaterialChip(icon: Int, name: Int, count: Int) {
    StatusChip(
        icon = icon,
        text = count.toString(),
        accessibilityLabel = stringResource(R.string.material_count_a11y, stringResource(name), count),
    )
}

@Composable
private fun GardenHero(totalDistanceKm: Double?, modifier: Modifier = Modifier) {
    val type = PlentyTheme.typography
    val colors = PlentyTheme.colors
    Column(modifier) {
        Text(stringResource(R.string.garden_eyebrow), style = type.label, color = colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.garden_title),
            style = type.title.copy(fontSize = 40.sp, lineHeight = 52.sp),
            color = colors.text,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.garden_body),
            style = type.body.copy(lineHeight = 38.sp),
            color = colors.textSecondary,
        )
        if (totalDistanceKm != null) {
            Spacer(Modifier.height(32.dp))
            StatusChip(
                icon = R.drawable.ic_route,
                text = stringResource(
                    R.string.garden_total_distance,
                    String.format(Locale.KOREA, "%.1f", totalDistanceKm),
                ),
                modifier = Modifier.width(240.dp),
            )
        }
    }
}

@Composable
private fun PotsPanel(
    state: GardenUiState,
    onPotClick: (Int) -> Unit,
    onEmptyPotClick: () -> Unit,
    onPrepareJourney: () -> Unit,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, PanelShape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .background(colors.surface.copy(alpha = 0.96f), PanelShape)
            .border(1.dp, colors.border, PanelShape)
            .padding(start = 28.dp, end = 32.dp, top = 18.dp, bottom = 28.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Row {
                Text(
                    text = stringResource(R.string.garden_pots_title),
                    style = type.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.text,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(44.dp))
                Text(
                    text = stringResource(R.string.garden_pots_count, state.filledPotCount, GardenLayout.SLOT_COUNT),
                    style = type.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                state.pots.forEachIndexed { index, pot ->
                    val slotNumber = index + 1
                    PotSlot(
                        number = slotNumber,
                        plant = pot?.art,
                        onClick = { if (pot != null) onPotClick(slotNumber) else onEmptyPotClick() },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GroveButton(
                text = stringResource(R.string.garden_prepare_journey),
                onClick = onPrepareJourney,
                modifier = Modifier.width(360.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.garden_prepare_journey_hint, GardenLayout.MAX_COMPANIONS),
                style = type.navLabel.copy(fontWeight = FontWeight.Normal),
                color = colors.textSecondary,
            )
        }
    }
}
