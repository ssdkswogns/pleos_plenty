package app.plenty.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.GardenLayout
import app.plenty.core.content.GameContent
import app.plenty.data.GardenSnapshot
import app.plenty.data.PottedPlant
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.collection.growthLine
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.plantDisplayName
import app.plenty.ui.theme.PlentyRadius
import app.plenty.ui.theme.PlentyTheme

private val CardShape = RoundedCornerShape(PlentyRadius.panel)

/**
 * 05 동반자 선택: 1..3 potted plants of any stage. The CTA only saves the ready selection; the
 * journey itself starts when the vehicle leaves park (INTERACTIONS.md §05/06). At the limit,
 * unselected cards are disabled but selected ones can still be cleared.
 */
@Composable
fun CompanionsScreen(
    garden: GardenSnapshot,
    initialSelection: List<Long>,
    busy: Boolean,
    backTo: String,
    content: GameContent,
    onBack: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
    onOpenBag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val max = GardenLayout.MAX_COMPANIONS
    var picked by rememberSaveable { mutableStateOf(initialSelection) }
    val potted = garden.plants.map { it.state.id }.toSet()
    // A ready plant could only vanish with a future delete feature; never submit unknown IDs.
    val selected = picked.filter { it in potted }

    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        DetailHeader(
            title = stringResource(R.string.companions_title),
            subtitle = stringResource(R.string.companions_subtitle, max),
            backTo = backTo,
            onBack = onBack,
        )
        Spacer(Modifier.height(32.dp))
        if (garden.plants.isEmpty()) {
            EmptyCompanions(onOpenBag, Modifier.weight(1f))
            return@Column
        }
        val cardHeight = if (garden.plants.size <= 3) 420.dp else 330.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(garden.plants, key = { it.state.id }) { plant ->
                val isSelected = plant.state.id in selected
                CompanionCard(
                    plant = plant,
                    selected = isSelected,
                    enabled = isSelected || selected.size < max,
                    content = content,
                    onToggle = {
                        picked = if (isSelected) selected - plant.state.id else selected + plant.state.id
                    },
                    modifier = Modifier.height(cardHeight),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        PlentyCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 40.dp, end = 40.dp, top = 28.dp, bottom = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.companions_count, selected.size, max), style = type.heading, color = colors.primary)
                    if (selected.size == max) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.companions_full, max), style = type.caption, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.width(24.dp))
                GroveButton(
                    text = stringResource(R.string.companions_cta),
                    onClick = { onConfirm(selected) },
                    enabled = selected.isNotEmpty() && !busy,
                    modifier = Modifier.width(340.dp),
                )
            }
        }
    }
}

@Composable
private fun CompanionCard(
    plant: PottedPlant,
    selected: Boolean,
    enabled: Boolean,
    content: GameContent,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val name = plantDisplayName(plant.state, content)
    val (detail, _) = growthLine(plant, content)
    val state = stringResource(if (selected) R.string.companion_selected else R.string.companion_not_selected)
    PlentyCard(
        modifier
            .fillMaxWidth()
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .clearAndSetSemantics {
                contentDescription = "$name, $detail"
                stateDescription = state
            },
    ) {
        if (selected) Box(Modifier.matchParentSize().border(2.dp, colors.accent, CardShape))
        Box(Modifier.matchParentSize().alpha(if (enabled) 1f else 0.45f)) {
            PlantImage(
                PlantArt.of(plant.state.species, plant.state.stage),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp).fillMaxHeight(0.58f).aspectRatio(1f),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(start = 28.dp, end = 28.dp, bottom = 32.dp)) {
                Text(name, style = type.label.copy(fontSize = 25.sp, fontWeight = FontWeight.Bold), color = colors.primary, maxLines = 1)
                Spacer(Modifier.height(12.dp))
                Text(detail, style = type.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
            }
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp)
                    .size(40.dp)
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun EmptyCompanions(onOpenBag: () -> Unit, modifier: Modifier) {
    val colors = PlentyTheme.colors
    PlentyCard(modifier.fillMaxWidth()) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_seed), contentDescription = null, tint = colors.accent, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.companions_empty_title), style = PlentyTheme.typography.heading, color = colors.primary)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.companions_empty_body), style = PlentyTheme.typography.body, color = colors.textSecondary)
            Spacer(Modifier.height(32.dp))
            GroveButton(stringResource(R.string.confirm_to_bag), onOpenBag, Modifier.width(300.dp))
        }
    }
}
