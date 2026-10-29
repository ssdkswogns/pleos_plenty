package app.plenty.ui.plant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.GardenLayout
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.SpeciesId
import app.plenty.data.GardenSnapshot
import app.plenty.data.ItemIds
import app.plenty.ui.ContentRes
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.GroveButtonStyle
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.potLabel
import app.plenty.ui.theme.PlentyTheme

private val TileShape = RoundedCornerShape(20.dp)

/**
 * 11 말랑 심기 확인 / 11b no empty pot. Preselects [preferredSlot] if empty, else the lowest empty
 * pot. Nothing is spent until [onConfirm]; if the chosen pot gets taken meanwhile the selection is
 * cleared and the user picks again (never auto-planted elsewhere).
 */
@Composable
fun PlantConfirmScreen(
    species: SpeciesId,
    garden: GardenSnapshot,
    preferredSlot: Int?,
    backTo: String,
    cancelLabel: String,
    busy: Boolean,
    /** Incremented by the caller each time a confirm found the garden or bag changed. */
    stateChangedKey: Int,
    onCancel: () -> Unit,
    onConfirm: (slot: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val occupied = garden.plants.map { it.slot }.toSet()
    val empty = (1..GardenLayout.SLOT_COUNT).filter { it !in occupied }
    val owned = garden.count(ItemIds.mallang(species))
    val initial = preferredSlot?.takeIf { it in empty } ?: empty.firstOrNull()
    var chosen by rememberSaveable { mutableStateOf(initial) }
    var stateChanged by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(stateChangedKey) {
        if (stateChangedKey > 0) {
            stateChanged = true
            chosen = null
        }
    }
    val selected = chosen?.takeIf { it in empty }
    val name = stringResource(R.string.mallang_name, stringResource(ContentRes.speciesName(species)))

    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        DetailHeader(
            title = stringResource(R.string.confirm_title),
            subtitle = stringResource(R.string.confirm_subtitle),
            backTo = backTo,
            onBack = onCancel,
        )
        Spacer(Modifier.height(32.dp))
        Row(Modifier.weight(1f).heightIn(max = 720.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            PlentyCard(Modifier.weight(470f).fillMaxHeight()) {
                PlantImage(
                    PlantArt.of(species, GrowthStage.SEED),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp).size(330.dp),
                )
                Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(name, style = type.title.copy(fontSize = 30.sp), color = colors.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.confirm_in_bag, owned), style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary)
                }
            }
            PlentyCard(Modifier.weight(726f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(40.dp)) {
                    Text(stringResource(R.string.confirm_pots_title), style = type.heading, color = colors.primary)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.confirm_pots_subtitle), style = type.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
                    Spacer(Modifier.height(28.dp))
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        (1..GardenLayout.SLOT_COUNT).chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { slot ->
                                    SlotTile(
                                        slot = slot,
                                        used = slot in occupied,
                                        selected = slot == selected,
                                        onSelect = {
                                            chosen = slot
                                            stateChanged = false
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    val (headline, detail) = when {
                        owned == 0 -> stringResource(R.string.confirm_no_mallang) to stringResource(R.string.confirm_hint)
                        empty.isEmpty() -> stringResource(R.string.confirm_no_slot) to stringResource(R.string.confirm_no_slot_hint)
                        stateChanged || selected == null -> stringResource(R.string.confirm_changed) to stringResource(R.string.confirm_hint)
                        else -> stringResource(R.string.confirm_summary, name, owned, owned - 1) to stringResource(R.string.confirm_hint)
                    }
                    Text(headline, style = type.label.copy(fontWeight = FontWeight.Medium), color = colors.primary)
                    Spacer(Modifier.height(10.dp))
                    Text(detail, style = type.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        GroveButton(cancelLabel, onCancel, Modifier.weight(250f), style = GroveButtonStyle.Secondary, trailingIcon = null)
                        GroveButton(
                            text = when {
                                empty.isEmpty() -> stringResource(R.string.confirm_cta_none)
                                selected == null -> stringResource(R.string.confirm_cta_pick)
                                else -> stringResource(R.string.confirm_cta, potLabel(selected))
                            },
                            onClick = { selected?.let(onConfirm) },
                            enabled = selected != null && owned > 0 && !busy,
                            modifier = Modifier.weight(376f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotTile(slot: Int, used: Boolean, selected: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val status = stringResource(
        when {
            used -> R.string.slot_used
            selected -> R.string.slot_selected
            else -> R.string.slot_empty
        },
    )
    Box(
        modifier
            .height(108.dp)
            .clip(TileShape)
            .background(if (selected) colors.surfaceTonal else colors.background)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.border, TileShape)
            .selectable(selected = selected, enabled = !used, role = Role.RadioButton, onClick = onSelect)
            .clearAndSetSemantics { contentDescription = "${potLabel(slot)} 화분, $status" },
    ) {
        Text(potLabel(slot), style = type.caption, color = colors.textSecondary, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
        Column(Modifier.fillMaxWidth().align(Alignment.Center).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(
                    when {
                        used -> R.drawable.ic_garden
                        selected -> R.drawable.ic_check
                        else -> R.drawable.ic_plus
                    },
                ),
                contentDescription = null,
                tint = if (used) colors.textSecondary else colors.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(status, style = type.caption, color = if (used) colors.textSecondary else colors.primary)
        }
    }
}
