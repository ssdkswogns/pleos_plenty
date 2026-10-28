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
import androidx.compose.foundation.layout.width
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
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.GrowthProgress
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.plantDisplayName
import app.plenty.ui.stageNameRes
import app.plenty.ui.theme.PlentyTheme

data class CollectionUiState(val entries: List<SpeciesEntry>) {
    val discoveredCount: Int get() = entries.count { it.best != null }
}

/** [best] is the most grown plant of the species; null = not met yet. */
data class SpeciesEntry(val species: SpeciesId, val best: PottedPlant?)

fun GardenSnapshot.toCollectionUiState(content: GameContent = DefaultContent) = CollectionUiState(
    entries = content.species.map { species ->
        SpeciesEntry(
            species = species,
            best = plants
                .filter { it.state.species == species }
                .maxWithOrNull(compareBy({ it.state.stage }, { it.state.xp })),
        )
    },
)

/** 15 식물도감: met species with their most grown plant, the rest locked. */
@Composable
fun CollectionScreen(
    state: CollectionUiState,
    onOpenPlant: (slot: Int) -> Unit,
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
            style = PlentyTheme.typography.body.copy(fontSize = 23.sp),
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            state.entries.forEach { entry ->
                Box(Modifier.weight(1f)) {
                    val best = entry.best
                    if (best != null) DiscoveredCard(best, content, onOpenPlant) else LockedCard(entry.species)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        PlentyCard(Modifier.fillMaxWidth().height(100.dp)) {
            Row(Modifier.fillMaxSize().padding(start = 31.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_book), null, tint = colors.accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(23.dp))
                Text(
                    stringResource(R.string.collection_banner),
                    style = PlentyTheme.typography.body.copy(fontSize = 25.sp, fontWeight = FontWeight.Medium),
                    color = colors.text,
                )
            }
        }
    }
}

@Composable
private fun DiscoveredCard(plant: PottedPlant, content: GameContent, onOpenPlant: (Int) -> Unit) {
    val colors = PlentyTheme.colors
    val state = plant.state
    val name = plantDisplayName(state, content)
    val stage = stringResource(stageNameRes(state.stage))
    // Thresholds are cumulative XP (GAME_RULES.md §2), so "37 / 100 XP" reads as progress to the next stage.
    val nextXp = when (state.stage) {
        GrowthStage.SEED -> content.growth.sproutXp
        GrowthStage.SPROUT -> content.growth.matureXp
        GrowthStage.MATURE -> null
    }
    val detail = if (nextXp != null) {
        stringResource(R.string.collection_progress, stage, state.xp, nextXp)
    } else {
        stringResource(R.string.collection_progress_done, stage, state.xp)
    }
    CardFrame(
        Modifier
            .clickable(role = Role.Button) { onOpenPlant(plant.slot) }
            .semantics { contentDescription = "$name, $detail" },
    ) {
        PlantImage(
            art = PlantArt.of(state.species, state.stage),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp).size(240.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 30.dp)
                .clearAndSetSemantics {},
        ) {
            Text(name, style = PlentyTheme.typography.heading.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold), color = colors.text, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(detail, style = PlentyTheme.typography.caption, color = colors.textSecondary)
            Spacer(Modifier.height(48.dp))
            GrowthProgress(progress = nextXp?.let { state.xp.toFloat() / it } ?: 1f)
        }
    }
}

@Composable
private fun LockedCard(species: SpeciesId) {
    val colors = PlentyTheme.colors
    val title = stringResource(R.string.collection_locked_title)
    val body = stringResource(R.string.collection_locked_body)
    CardFrame(Modifier.clearAndSetSemantics { contentDescription = "$title. $body" }) {
        Box(Modifier.align(Alignment.TopCenter).padding(top = 32.dp).size(240.dp), contentAlignment = Alignment.Center) {
            PlantImage(
                art = PlantArt.of(species, GrowthStage.MATURE),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.18f),
            )
            Icon(painterResource(R.drawable.ic_lock), null, tint = colors.primary, modifier = Modifier.size(40.dp))
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, end = 28.dp, bottom = 92.dp),
        ) {
            Text(title, style = PlentyTheme.typography.heading.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(body, style = PlentyTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun CardFrame(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    PlentyCard(modifier.fillMaxWidth().height(455.dp), content = content)
}
