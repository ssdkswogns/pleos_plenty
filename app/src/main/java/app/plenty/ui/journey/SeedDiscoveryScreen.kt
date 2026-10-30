package app.plenty.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.SpeciesId
import app.plenty.ui.ContentRes
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.GroveButtonStyle
import app.plenty.ui.theme.PlentyTheme

/**
 * 09 말랑 발견: the 말랑 the settlement already put in the bag. Planting goes through 11; "나중에
 * 심기" leaves it in the bag. Without an empty pot the plant action is disabled with a hint.
 */
@Composable
fun SeedDiscoveryScreen(
    species: SpeciesId,
    inBag: Int,
    hasEmptyPot: Boolean,
    onBack: () -> Unit,
    onPlant: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        DetailHeader(
            title = stringResource(R.string.discovery_title),
            subtitle = stringResource(R.string.discovery_subtitle),
            backTo = stringResource(R.string.screen_result),
            onBack = onBack,
        )
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier
                    .size(520.dp, 360.dp)
                    .background(Brush.radialGradient(listOf(Color.White, colors.surfaceTonal.copy(alpha = 0f)))),
                contentAlignment = Alignment.Center,
            ) {
                PlantImage(PlantArt.of(species, GrowthStage.SEED), contentDescription = null, modifier = Modifier.size(330.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.mallang_name, stringResource(ContentRes.speciesName(species))),
                style = type.display.copy(fontSize = 40.sp, lineHeight = 52.sp),
                color = colors.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.discovery_body), style = type.body, color = colors.textSecondary)
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.background(colors.surfaceTonal, CircleShape).padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_bag), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.discovery_in_bag, inBag), style = type.caption.copy(fontWeight = FontWeight.Medium), color = colors.primary)
            }
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                GroveButton(
                    stringResource(R.string.discovery_plant),
                    onPlant,
                    Modifier.width(370.dp),
                    enabled = hasEmptyPot && inBag > 0,
                )
                GroveButton(
                    stringResource(R.string.discovery_later),
                    onLater,
                    Modifier.width(270.dp),
                    style = GroveButtonStyle.Secondary,
                    trailingIcon = null,
                )
            }
            if (!hasEmptyPot) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.discovery_no_slot), style = type.caption.copy(fontSize = 20.sp), color = colors.textSecondary)
            }
        }
    }
}
