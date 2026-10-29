package app.plenty.ui.plant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialId
import app.plenty.core.content.TraitId
import app.plenty.core.rules.GrowthEngine
import app.plenty.data.GardenSnapshot
import app.plenty.data.ItemIds
import app.plenty.data.PottedPlant
import app.plenty.ui.ContentRes
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.components.QuantityStepper
import app.plenty.ui.components.TraitScoreRows
import app.plenty.ui.materialsInOrder
import app.plenty.ui.nextStageName
import app.plenty.ui.plantDisplayName
import app.plenty.ui.theme.PlentyTheme

private val OptionShape = RoundedCornerShape(24.dp)

/**
 * 12 특성 가꾸기: choose a material and count; scores and the predicted next name are previewed in
 * memory with the engine's own rule. Nothing is spent until [onApply]; XP and the current name never
 * change here.
 */
@Composable
fun TraitTuningScreen(
    plant: PottedPlant,
    garden: GardenSnapshot,
    content: GameContent,
    busy: Boolean,
    onBack: () -> Unit,
    onApply: (MaterialId, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val state = plant.state
    val engine = remember(content) { GrowthEngine(content) }
    val materials = content.materialsInOrder()
    val owned = materials.associateWith { garden.count(ItemIds.material(it)) }
    // First owned material in content order, count 1 (INTERACTIONS.md §12).
    var selectedId by rememberSaveable { mutableStateOf(materials.firstOrNull { (owned[it] ?: 0) > 0 }?.value) }
    val selected = materials.firstOrNull { it.value == selectedId && (owned[it] ?: 0) > 0 }
    var count by rememberSaveable { mutableIntStateOf(1) }
    val maxCount = selected?.let { owned.getValue(it) } ?: 0
    val effectiveCount = count.coerceIn(if (maxCount > 0) 1 else 0, maxCount)

    val points = state.stageTraitPoints
    val preview: Map<TraitId, Int>? = selected?.takeIf { effectiveCount > 0 }?.let {
        engine.applyMaterial(state, it, effectiveCount).stageTraitPoints
    }
    val boostedTrait = selected?.let(content::traitBoostedBy)

    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        DetailHeader(
            title = stringResource(R.string.tune_title),
            subtitle = stringResource(R.string.tune_subtitle, plantDisplayName(state, content)),
            backTo = stringResource(R.string.screen_plant_detail),
            onBack = onBack,
        )
        Spacer(Modifier.height(32.dp))
        Row(Modifier.weight(1f).heightIn(max = 720.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            PlentyCard(Modifier.weight(552f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(40.dp)) {
                    Text(stringResource(R.string.tune_pick), style = type.heading, color = colors.primary)
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        materials.forEach { material ->
                            MaterialOption(
                                material = material,
                                traitLabel = content.traitBoostedBy(material)?.label.orEmpty(),
                                points = content.growth.traitPointsPerMaterial,
                                owned = owned.getValue(material),
                                selected = material == selected,
                                onSelect = {
                                    selectedId = material.value
                                    count = 1
                                },
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.tune_quantity), style = type.label.copy(fontWeight = FontWeight.Medium), color = colors.text, modifier = Modifier.weight(1f))
                        QuantityStepper(
                            value = effectiveCount,
                            range = if (maxCount > 0) 1..maxCount else 0..0,
                            onChange = { count = it },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = selected?.let {
                            stringResource(R.string.tune_usage, stringResource(ContentRes.materialName(it)), maxCount, effectiveCount)
                        } ?: stringResource(R.string.tune_no_material),
                        style = type.caption.copy(fontSize = 20.sp),
                        color = colors.textSecondary,
                    )
                }
            }
            PlentyCard(Modifier.weight(644f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(40.dp)) {
                    Text(stringResource(R.string.tune_changes), style = type.heading, color = colors.primary)
                    Spacer(Modifier.height(24.dp))
                    TraitScoreRows(content, points, preview = preview)
                    Spacer(Modifier.weight(1f))
                    val predictedNow = nextStageName(state, points, content)
                    val predicted = nextStageName(state, preview ?: points, content)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceTonal, OptionShape)
                            .padding(horizontal = 24.dp, vertical = 22.dp),
                    ) {
                        Text(stringResource(R.string.tune_predicted), style = type.caption, color = colors.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        Text(predicted.orEmpty(), style = type.heading, color = colors.primary)
                    }
                    Spacer(Modifier.height(14.dp))
                    val after = preview ?: points
                    val top = after.values.maxOrNull() ?: 0
                    val hint = when {
                        top == 0 -> stringResource(R.string.no_points_hint)
                        after.values.count { it == top } > 1 -> stringResource(R.string.tune_tie)
                        preview != null && predicted == predictedNow && boostedTrait != null ->
                            stringResource(R.string.tune_same_name, boostedTrait.label)
                        state.stage == GrowthStage.SEED -> stringResource(R.string.tune_note_seed)
                        else -> stringResource(R.string.tune_note_sprout)
                    }
                    Text(hint, style = type.caption, color = colors.textSecondary)
                    Spacer(Modifier.height(18.dp))
                    GroveButton(
                        text = selected?.let {
                            stringResource(R.string.tune_cta, stringResource(ContentRes.materialName(it)), effectiveCount)
                        } ?: stringResource(R.string.tune_cta_none),
                        onClick = { selected?.let { onApply(it, effectiveCount) } },
                        enabled = selected != null && effectiveCount > 0 && !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialOption(
    material: MaterialId,
    traitLabel: String,
    points: Int,
    owned: Int,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val enabled = owned > 0
    Row(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(OptionShape)
            .background(if (selected) colors.surfaceTonal else colors.background)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.border, OptionShape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (enabled) colors.accent else colors.textSecondary
        Icon(painterResource(ContentRes.materialIcon(material)), null, tint = tint, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(22.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(ContentRes.materialName(material)), style = type.body.copy(fontWeight = FontWeight.SemiBold), color = if (enabled) colors.primary else colors.textSecondary)
            Text(stringResource(R.string.tune_material_effect, traitLabel, points), style = type.caption, color = colors.textSecondary)
        }
        Text(
            text = stringResource(if (enabled) R.string.tune_owned else R.string.tune_owned_zero, owned),
            style = type.label.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) colors.primary else colors.textSecondary,
        )
    }
}
