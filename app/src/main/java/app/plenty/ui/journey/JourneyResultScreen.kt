package app.plenty.ui.journey

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.Trait
import app.plenty.data.GrowthResult
import app.plenty.data.JourneyResult
import app.plenty.data.JourneyResultState
import app.plenty.data.JourneySource
import app.plenty.ui.ContentRes
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.components.TraitBadge
import app.plenty.ui.formatKm
import app.plenty.ui.grownPlantName
import app.plenty.ui.theme.PlentyTheme
import app.plenty.ui.trait
import kotlin.math.floor

/**
 * 08 여정 결과, read back from the stored settlement (INTERACTIONS.md §08): distance, each
 * companion's actual XP and stage change, and the region reward that settleJourney already put in
 * the bag. Nothing here grants, rolls or unlocks anything; "말랑 만나기" only opens 09.
 */
@Composable
fun JourneyResultScreen(
    result: JourneyResult,
    content: GameContent,
    onBack: () -> Unit,
    onMeet: () -> Unit,
    onGarden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settled = result.state == JourneyResultState.Settled
    val region = result.arrivalRegion?.let(content::region)
    val place = when {
        !settled -> startedAtText(result.startedAt)
        region != null -> stringResource(R.string.result_region, region.name)
        else -> stringResource(R.string.result_region_unknown)
    }
    val subtitle = if (result.source == JourneySource.MOCK) "$place · ${stringResource(R.string.result_mock_note)}" else place

    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        Box(Modifier.fillMaxWidth()) {
            DetailHeader(
                title = stringResource(if (settled) R.string.result_title else R.string.screen_result),
                subtitle = subtitle,
                backTo = stringResource(R.string.nav_journey),
                onBack = onBack,
            )
            val trait = content.trait(region?.trait)
            if (settled && trait != null) {
                TraitBadge(trait, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 68.dp))
            }
        }
        Spacer(Modifier.height(32.dp))
        when (result.state) {
            JourneyResultState.Settled -> SettledBody(result, content, onMeet, onGarden, Modifier.weight(1f))
            JourneyResultState.CheckingDistance ->
                StatusBody(R.drawable.ic_refresh, R.string.result_checking_title, R.string.result_checking_body, onGarden, Modifier.weight(1f))
            JourneyResultState.Underway ->
                StatusBody(R.drawable.ic_route, R.string.result_underway_title, R.string.result_underway_body, onGarden, Modifier.weight(1f))
            JourneyResultState.Rejected ->
                StatusBody(R.drawable.ic_info, R.string.result_rejected_title, R.string.result_rejected_body, onGarden, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SettledBody(result: JourneyResult, content: GameContent, onMeet: () -> Unit, onGarden: () -> Unit, modifier: Modifier) {
    val km = result.distanceKm ?: 0.0
    val regionTrait = content.trait(result.arrivalRegion?.let(content::region)?.trait)
    Column(modifier) {
        Row(Modifier.weight(1f, fill = false).heightIn(max = 470.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            DistanceCard(km, result.growth, Modifier.weight(374f).fillMaxHeight())
            result.growth.forEach { growth ->
                GrowthCard(growth, km, regionTrait, content, Modifier.weight(400f).fillMaxHeight())
            }
            // A lone companion keeps the two-companion card width instead of stretching.
            repeat((2 - result.growth.size).coerceAtLeast(0)) { Spacer(Modifier.weight(400f)) }
        }
        Spacer(Modifier.height(32.dp))
        RewardBar(result, onMeet, onGarden)
    }
}

@Composable
private fun DistanceCard(km: Double, growth: List<GrowthResult>, modifier: Modifier) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val distance = formatKm(km)
    val spoken = stringResource(R.string.result_km_a11y, distance)
    // A shared "+N XP" line only when several companions really got the same positive amount;
    // otherwise each card says it (daily caps differ per plant).
    val sameXp = growth.map { it.xpGained }.distinct().singleOrNull()?.takeIf { it > 0 && growth.size > 1 }
    PlentyCard(modifier.semantics(mergeDescendants = true) {}) {
        Column(Modifier.fillMaxSize().padding(40.dp)) {
            Icon(painterResource(R.drawable.ic_route), contentDescription = null, tint = colors.accent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(40.dp))
            Text(stringResource(R.string.result_distance_label), style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            ) {
                Text(distance, style = type.display.copy(fontSize = 84.sp, lineHeight = 96.sp), color = colors.primary)
                Spacer(Modifier.width(20.dp))
                Text(
                    stringResource(R.string.result_km),
                    style = type.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (sameXp != null) {
                Text(stringResource(R.string.result_each_xp, sameXp), style = type.heading.copy(fontSize = 25.sp), color = colors.accent)
            }
        }
    }
}

@Composable
private fun GrowthCard(growth: GrowthResult, km: Double, regionTrait: Trait?, content: GameContent, modifier: Modifier) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val xpLine = if (growth.xpBefore != null && growth.xpAfter != null) {
        stringResource(R.string.result_xp_change, growth.xpBefore, growth.xpAfter)
    } else {
        stringResource(R.string.result_xp_gained, growth.xpGained)
    }
    val notes = buildList {
        if (growth.grew) {
            add(
                stringResource(
                    R.string.stage_change,
                    stringResource(ContentRes.stageName(growth.stageBefore)),
                    stringResource(ContentRes.stageName(growth.stageAfter)),
                ),
            )
        }
        // xp = floor(km + remainder) >= floor(km) unless the daily cap cut it.
        if (growth.xpGained < floor(km).toInt()) add(stringResource(R.string.result_daily_cap))
        if (growth.stageBefore == GrowthStage.MATURE) add(stringResource(R.string.result_mature_kept))
    }
    PlentyCard(modifier.semantics(mergeDescendants = true) {}) {
        PlantImage(
            PlantArt.of(growth.species, growth.stageAfter),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp).size(190.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 28.dp, end = 28.dp, bottom = 36.dp)) {
            Text(
                grownPlantName(growth.species, growth.stageAfter, growth.prefixAfter, content),
                style = type.label.copy(fontSize = 25.sp, fontWeight = FontWeight.Bold),
                color = colors.primary,
                maxLines = 1,
            )
            Spacer(Modifier.height(16.dp))
            Text(xpLine, style = type.heading, color = colors.accent)
            Spacer(Modifier.height(14.dp))
            // A journey only raises the arrival region's trait, and mature plants gain none.
            if (regionTrait != null && growth.traitPoints > 0) {
                Text(
                    stringResource(R.string.result_trait_points, regionTrait.label, growth.traitPoints),
                    style = type.caption,
                    color = colors.textSecondary,
                )
            }
            notes.forEach { Text(it, style = type.caption, color = colors.textSecondary) }
        }
    }
}

@Composable
private fun RewardBar(result: JourneyResult, onMeet: () -> Unit, onGarden: () -> Unit) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val parts = buildList {
        result.seed?.let { add(stringResource(R.string.reward_mallang, stringResource(ContentRes.speciesName(it)), 1)) }
        result.material?.let { add(stringResource(R.string.reward_material, stringResource(ContentRes.materialName(it)), result.materialCount)) }
    }
    val (title, note) = when {
        parts.isNotEmpty() -> parts.joinToString(" · ") to stringResource(R.string.result_rewards_note)
        // No reward is not a hint that the region was unknown: a same-day revisit pays none either.
        result.arrivalRegion == null -> stringResource(R.string.result_no_reward) to stringResource(R.string.result_no_reward_unknown)
        else -> stringResource(R.string.result_no_reward) to stringResource(R.string.result_no_reward_today)
    }
    PlentyCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 40.dp, vertical = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_seed), contentDescription = null, tint = colors.accent, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(title, style = type.heading, color = colors.primary)
                Spacer(Modifier.height(10.dp))
                Text(note, style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary)
            }
            Spacer(Modifier.width(24.dp))
            if (result.seed != null) {
                GroveButton(stringResource(R.string.result_meet), onMeet, Modifier.width(300.dp))
            } else {
                GroveButton(stringResource(R.string.result_to_garden), onGarden, Modifier.width(300.dp))
            }
        }
    }
}

/** Pending, still underway or rejected: no distance, XP or reward is shown (never "0 km"). */
@Composable
private fun StatusBody(@DrawableRes icon: Int, title: Int, body: Int, onGarden: () -> Unit, modifier: Modifier) {
    val colors = PlentyTheme.colors
    PlentyCard(modifier.fillMaxWidth().heightIn(max = 560.dp)) {
        Column(Modifier.align(Alignment.Center).padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(88.dp).background(colors.surfaceTonal, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(stringResource(title), style = PlentyTheme.typography.heading.copy(fontSize = 32.sp), color = colors.primary)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(body), style = PlentyTheme.typography.body, color = colors.textSecondary)
            Spacer(Modifier.height(40.dp))
            GroveButton(stringResource(R.string.result_to_garden), onGarden, Modifier.width(320.dp))
        }
    }
}
