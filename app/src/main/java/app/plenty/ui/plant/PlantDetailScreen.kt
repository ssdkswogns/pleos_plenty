package app.plenty.ui.plant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.data.PottedPlant
import app.plenty.ui.ContentRes
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.GroveButtonStyle
import app.plenty.ui.components.GrowthProgress
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.components.TraitBadge
import app.plenty.ui.components.TraitBadgeStyle
import app.plenty.ui.components.TraitScoreRows
import app.plenty.ui.nextStageName
import app.plenty.ui.nextStageXp
import app.plenty.ui.plantDisplayName
import app.plenty.ui.potLabel
import app.plenty.ui.theme.PlentyTheme
import app.plenty.ui.trait

/** 10 식물 상세 (seed/sprout) and 10b (mature): current name, growth, current-stage trait points. */
@Composable
fun PlantDetailScreen(
    plant: PottedPlant,
    content: GameContent,
    onBack: () -> Unit,
    onTune: () -> Unit,
    onCompanions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = plant.state
    val mature = state.stage == GrowthStage.MATURE
    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        DetailHeader(
            title = plantDisplayName(state, content),
            subtitle = stringResource(
                if (mature) R.string.detail_subtitle_mature else R.string.detail_subtitle_growing,
                potLabel(plant.slot),
            ),
            backTo = stringResource(R.string.nav_garden),
            onBack = onBack,
        )
        Spacer(Modifier.height(32.dp))
        Row(Modifier.weight(1f).heightIn(max = 720.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            PlentyCard(Modifier.weight(470f).fillMaxHeight()) { PlantPortrait(plant, content) }
            PlentyCard(Modifier.weight(726f).fillMaxHeight()) {
                if (mature) MatureSummary(plant, content, onCompanions) else GrowthSummary(plant, content, onTune, onCompanions)
            }
        }
    }
}

/** Plant art over a soft halo, the current prefix badge (if any) and "새싹 · 누적 37 XP". */
@Composable
internal fun BoxScope.PlantPortrait(plant: PottedPlant, content: GameContent) {
    val state = plant.state
    val prefix = when (state.stage) {
        GrowthStage.SEED -> null
        GrowthStage.SPROUT -> content.trait(state.sproutPrefix)
        GrowthStage.MATURE -> content.trait(state.maturePrefix)
    }
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .padding(top = 36.dp)
            .size(440.dp)
            .background(Brush.radialGradient(listOf(Color.White, PlentyTheme.colors.surfaceTonal.copy(alpha = 0f)))),
        contentAlignment = Alignment.Center,
    ) {
        PlantImage(PlantArt.of(state.species, state.stage), contentDescription = null, modifier = Modifier.size(350.dp))
    }
    Column(
        Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (prefix != null) {
            TraitBadge(prefix, TraitBadgeStyle.Prefix)
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text = stringResource(R.string.detail_stage_xp, stringResource(ContentRes.stageName(state.stage)), state.xp),
            style = PlentyTheme.typography.label,
            color = PlentyTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun GrowthSummary(plant: PottedPlant, content: GameContent, onTune: () -> Unit, onCompanions: () -> Unit) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val state = plant.state
    val nextXp = checkNotNull(content.nextStageXp(state.stage))
    val nextStage = stringResource(ContentRes.stageName(if (state.stage == GrowthStage.SEED) GrowthStage.SPROUT else GrowthStage.MATURE))
    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.detail_until_next, nextStage, (nextXp - state.xp).coerceAtLeast(0)),
                style = type.heading,
                color = colors.primary,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.detail_xp_of, state.xp, nextXp), style = type.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
        }
        Spacer(Modifier.height(20.dp))
        GrowthProgress(progress = state.xp.toFloat() / nextXp)
        Spacer(Modifier.height(40.dp))
        Text(stringResource(R.string.detail_scores_title), style = type.label.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
        Spacer(Modifier.height(16.dp))
        TraitScoreRows(content, state.stageTraitPoints)
        if (state.stageTraitPoints.values.all { it == 0 }) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.no_points_hint), style = type.caption, color = colors.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.detail_predicted_label), style = type.caption, color = colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        Text(nextStageName(state, content = content).orEmpty(), style = type.heading, color = colors.primary)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GroveButton(stringResource(R.string.detail_tune), onTune, Modifier.weight(390f))
            GroveButton(
                text = stringResource(R.string.detail_companions),
                onClick = onCompanions,
                modifier = Modifier.weight(240f),
                style = GroveButtonStyle.Secondary,
                trailingIcon = null,
            )
        }
    }
}

@Composable
private fun MatureSummary(plant: PottedPlant, content: GameContent, onCompanions: () -> Unit) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    Column(Modifier.fillMaxSize().padding(40.dp)) {
        Row(
            Modifier.background(colors.surfaceTonal, CircleShape).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_check), null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.mature_badge), style = type.caption.copy(fontWeight = FontWeight.Medium), color = colors.primary)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.mature_title), style = type.title.copy(fontSize = 32.sp), color = colors.primary)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.mature_body), style = type.body.copy(lineHeight = 38.sp), color = colors.textSecondary)
        Spacer(Modifier.height(40.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.background, RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Text(stringResource(R.string.mature_confirmed_label), style = type.caption, color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            Text(plantDisplayName(plant.state, content), style = type.heading, color = colors.primary)
        }
        Spacer(Modifier.weight(1f))
        GroveButton(stringResource(R.string.mature_companions), onCompanions, Modifier.fillMaxWidth())
    }
}
