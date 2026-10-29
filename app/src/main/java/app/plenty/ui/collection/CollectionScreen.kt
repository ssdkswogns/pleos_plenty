package app.plenty.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.DefaultContent
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.SpeciesId
import app.plenty.data.GardenSnapshot
import app.plenty.data.PottedPlant
import app.plenty.ui.ContentRes
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.GrowthProgress
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.nextStageXp
import app.plenty.ui.theme.PlentyTheme

data class CollectionUiState(val entries: List<SpeciesEntry>) {
    val discoveredCount: Int get() = entries.count { it.best != null }
}

/**
 * Keyed by species, never by slot. [best] is the representative plant: highest stage, then most XP,
 * then lowest plantId (DECISIONS.md §4). Null = never planted = not met.
 */
data class SpeciesEntry(val species: SpeciesId, val best: PottedPlant?, val plantedCount: Int)

private val Representative = compareByDescending<PottedPlant> { it.state.stage }
    .thenByDescending { it.state.xp }
    .thenBy { it.state.id }

fun GardenSnapshot.toCollectionUiState(content: GameContent = DefaultContent) = CollectionUiState(
    entries = content.species.map { species ->
        val ofSpecies = plants.filter { it.state.species == species }
        SpeciesEntry(species, ofSpecies.minWithOrNull(Representative), ofSpecies.size)
    },
)

/**
 * "말랑 · 7 / 20 XP", "새싹 · 37 / 100 XP", "성체 · 누적 128 XP" with a bar of xp / next threshold
 * (1.0 when mature), so label and bar always agree (DECISIONS.md §4).
 */
@Composable
internal fun growthLine(entry: PottedPlant, content: GameContent): Pair<String, Float> {
    val state = entry.state
    val stage = stringResource(ContentRes.stageName(state.stage))
    val next = content.nextStageXp(state.stage)
    return if (next != null) {
        stringResource(R.string.collection_progress, stage, state.xp, next) to state.xp.toFloat() / next
    } else {
        stringResource(R.string.collection_progress_done, stage, state.xp) to 1f
    }
}

/** 15 식물도감: species met (planted at least once) show their most grown plant; the rest are locked. */
@Composable
fun CollectionScreen(
    state: CollectionUiState,
    onOpenSpecies: (SpeciesId) -> Unit,
    modifier: Modifier = Modifier,
    content: GameContent = DefaultContent,
) {
    val colors = PlentyTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 4.dp, top = 36.dp, bottom = 24.dp),
    ) {
        Text(stringResource(R.string.collection_title), style = PlentyTheme.typography.title, color = colors.text)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.collection_subtitle, state.discoveredCount, state.entries.size),
            style = PlentyTheme.typography.body.copy(fontSize = 22.sp),
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            state.entries.forEach { entry ->
                Box(Modifier.weight(1f)) {
                    val best = entry.best
                    if (best != null) DiscoveredCard(entry.species, best, content, onOpenSpecies) else LockedCard(entry.species)
                }
            }
        }
        Spacer(Modifier.height(64.dp))
        Text(stringResource(R.string.collection_note), style = PlentyTheme.typography.body.copy(fontSize = 22.sp), color = colors.textSecondary)
    }
}

@Composable
private fun DiscoveredCard(species: SpeciesId, best: PottedPlant, content: GameContent, onOpen: (SpeciesId) -> Unit) {
    val colors = PlentyTheme.colors
    val title = stringResource(ContentRes.speciesName(species))
    val (detail, progress) = growthLine(best, content)
    CardFrame(
        Modifier
            .clickable(role = Role.Button) { onOpen(species) }
            .semantics { contentDescription = "$title, $detail" },
    ) {
        PlantImage(
            art = PlantArt.of(species, best.state.stage),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp).size(250.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 28.dp)
                .clearAndSetSemantics {},
        ) {
            Text(title, style = PlentyTheme.typography.heading, color = colors.primary)
            Spacer(Modifier.height(8.dp))
            Text(detail, style = PlentyTheme.typography.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
            Spacer(Modifier.height(44.dp))
            GrowthProgress(progress = progress)
        }
    }
}

/** Not met yet: lock + hint only, and no navigation (DECISIONS.md §4). */
@Composable
private fun LockedCard(species: SpeciesId) {
    val colors = PlentyTheme.colors
    val title = stringResource(R.string.collection_locked_title)
    val body = stringResource(R.string.collection_locked_body)
    CardFrame(Modifier.clearAndSetSemantics { contentDescription = "$title. $body" }) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = 32.dp).size(250.dp), contentAlignment = Alignment.Center) {
            PlantImage(
                art = PlantArt.of(species, GrowthStage.MATURE),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.18f),
            )
            Icon(painterResource(R.drawable.ic_lock), null, tint = colors.primary, modifier = Modifier.size(40.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 28.dp, end = 28.dp, bottom = 82.dp)) {
            Text(title, style = PlentyTheme.typography.heading.copy(fontWeight = FontWeight.Bold), color = colors.primary)
            Spacer(Modifier.height(8.dp))
            Text(body, style = PlentyTheme.typography.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
        }
    }
}

@Composable
private fun CardFrame(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    PlentyCard(modifier.fillMaxWidth().height(460.dp), content = content)
}
