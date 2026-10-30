package app.plenty.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.data.GrowthResult
import app.plenty.ui.ContentRes
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.GroveButtonStyle
import app.plenty.ui.grownPlantName
import app.plenty.ui.theme.PlentyTheme

/**
 * 13 성장 완료: automatic growth from the stored before/after stages, one plant at a time with a
 * skip to the result. No choice, no material, no extra cost. A seed that passed both thresholds
 * shows "말랑 → 성체" and the final name the engine fixed.
 */
@Composable
fun GrowthCompleteScreen(
    grown: List<GrowthResult>,
    content: GameContent,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    var index by rememberSaveable { mutableIntStateOf(0) }
    val growth = grown.getOrNull(index) ?: return
    val last = index == grown.lastIndex
    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        DetailHeader(
            title = stringResource(R.string.growth_title),
            subtitle = stringResource(R.string.growth_subtitle),
            backTo = stringResource(R.string.screen_result),
            onBack = onDone,
        )
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            PlantImage(PlantArt.of(growth.species, growth.stageAfter), contentDescription = null, modifier = Modifier.size(330.dp))
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.background(colors.surfaceTonal, CircleShape).padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_spark), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(
                        R.string.stage_change,
                        stringResource(ContentRes.stageName(growth.stageBefore)),
                        stringResource(ContentRes.stageName(growth.stageAfter)),
                    ),
                    style = type.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.primary,
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                grownPlantName(growth.species, growth.stageAfter, growth.prefixAfter, content),
                style = type.display.copy(fontSize = 40.sp, lineHeight = 52.sp),
                color = colors.primary,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(if (growth.prefixAfter != null) R.string.growth_named else R.string.growth_unnamed),
                style = type.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(36.dp))
            if (last) {
                GroveButton(stringResource(R.string.growth_to_result), onDone, Modifier.width(360.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    GroveButton(stringResource(R.string.growth_skip), onDone, Modifier.width(240.dp), style = GroveButtonStyle.Secondary, trailingIcon = null)
                    GroveButton(stringResource(R.string.growth_next), { index++ }, Modifier.width(360.dp))
                }
            }
            if (grown.size > 1) {
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.growth_position, index + 1, grown.size), style = type.caption, color = colors.textSecondary)
            }
        }
    }
}
