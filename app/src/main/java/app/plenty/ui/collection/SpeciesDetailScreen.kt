package app.plenty.ui.collection

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.SpeciesId
import app.plenty.ui.ContentRes
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.components.TraitBadge
import app.plenty.ui.components.TraitBadgeStyle
import app.plenty.ui.plantDisplayName
import app.plenty.ui.theme.PlentyTheme
import app.plenty.ui.trait

/**
 * 15a 종 상세: read-only. Stages up to the representative plant's stage are shown, later ones are a
 * faded silhouette with a lock. No planting, material or companion actions here (INTERACTIONS.md §15a).
 */
@Composable
fun SpeciesDetailScreen(entry: SpeciesEntry, content: GameContent, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val best = checkNotNull(entry.best) { "species detail needs a met species" }
    val species = entry.species
    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        DetailHeader(
            title = stringResource(ContentRes.speciesName(species)),
            subtitle = stringResource(R.string.species_subtitle),
            backTo = stringResource(R.string.collection_title),
            onBack = onBack,
        )
        Spacer(Modifier.height(32.dp))
        Row(Modifier.weight(1f).heightIn(max = 720.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            PlentyCard(Modifier.weight(410f).fillMaxHeight()) {
                PlantImage(
                    PlantArt.of(species, best.state.stage),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp).size(350.dp),
                )
                Column(
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 28.dp).padding(bottom = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(ContentRes.speciesTagline(species)), style = type.body.copy(fontWeight = FontWeight.SemiBold), color = colors.primary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.species_count, entry.plantedCount), style = type.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
                }
            }
            PlentyCard(Modifier.weight(786f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(40.dp)) {
                    Text(stringResource(R.string.species_stages_title), style = type.heading, color = colors.primary)
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        GrowthStage.entries.forEach { stage ->
                            StageTile(species, stage, reached = stage <= best.state.stage, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.species_best_label), style = type.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val prefix = when (best.state.stage) {
                            GrowthStage.SEED -> null
                            GrowthStage.SPROUT -> content.trait(best.state.sproutPrefix)
                            GrowthStage.MATURE -> content.trait(best.state.maturePrefix)
                        }
                        if (prefix != null) {
                            TraitBadge(prefix, TraitBadgeStyle.Prefix)
                            Spacer(Modifier.width(22.dp))
                        }
                        Text(plantDisplayName(best.state, content), style = type.heading.copy(fontSize = 30.sp), color = colors.primary)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(growthLine(best, content).first, style = type.body, color = colors.text)
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(R.string.species_readonly), style = type.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun StageTile(species: SpeciesId, stage: GrowthStage, reached: Boolean, modifier: Modifier) {
    val colors = PlentyTheme.colors
    val stageName = stringResource(ContentRes.stageName(stage))
    val label = if (reached) stageName else stringResource(R.string.species_stage_locked, stageName)
    Column(
        modifier
            .height(224.dp)
            .background(colors.background, RoundedCornerShape(24.dp))
            .clearAndSetSemantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(top = 16.dp).size(160.dp), contentAlignment = Alignment.Center) {
            PlantImage(
                PlantArt.of(species, stage),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(if (reached) 1f else 0.18f),
            )
            if (!reached) Icon(painterResource(R.drawable.ic_lock), null, tint = colors.primary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(stageName, style = PlentyTheme.typography.label.copy(fontWeight = FontWeight.SemiBold), color = if (reached) colors.primary else colors.textSecondary)
    }
}
