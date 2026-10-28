package app.plenty.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.plenty.R
import app.plenty.ui.theme.PlentyRadius
import app.plenty.ui.theme.PlentyTheme
import app.plenty.ui.theme.PlentyTouch

/** Main destinations (HANDOFF.md §4). Settings is a separate entry point at the rail bottom. */
enum class TopDestination(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    Garden("garden", R.string.nav_garden, R.drawable.ic_garden),
    Collection("collection", R.string.nav_collection, R.drawable.ic_book),
    Bag("bag", R.string.nav_bag, R.drawable.ic_bag),
    Journey("journey", R.string.nav_journey, R.drawable.ic_route),
}

private val RailShape = RoundedCornerShape(PlentyRadius.rail)
private val ItemShape = RoundedCornerShape(22.dp)

/** Landscape navigation rail: 96 wide, 72x92 items spaced 138 apart (03-home.svg). */
@Composable
fun GroveNavigationRail(
    selected: TopDestination?,
    onSelect: (TopDestination) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    Column(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight()
            .shadow(elevation = 12.dp, shape = RailShape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .background(colors.surface.copy(alpha = 0.95f), RailShape)
            .padding(top = 22.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(46.dp),
        ) {
            TopDestination.entries.forEach { destination ->
                RailItem(
                    destination = destination,
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(PlentyTouch.minimumTarget)
                .clip(ItemShape)
                .clickable(role = Role.Button, onClick = onSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.nav_settings),
                tint = colors.textSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun RailItem(
    destination: TopDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = PlentyTheme.colors
    val content = if (selected) colors.onPrimary else colors.textSecondary
    Column(
        modifier = Modifier
            .width(72.dp)
            .height(92.dp)
            .clip(ItemShape)
            .background(if (selected) colors.primary else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(destination.icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(destination.label),
            style = PlentyTheme.typography.navLabel,
            color = content,
        )
    }
}
