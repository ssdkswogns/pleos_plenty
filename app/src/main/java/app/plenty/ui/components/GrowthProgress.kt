package app.plenty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.plenty.ui.theme.PlentyTheme

private val BarShape = RoundedCornerShape(5.dp)

/**
 * GrowthProgress: 10 tall, blue fill on a gray-blue track. Always shown next to the numbers in text;
 * [progress] null means unknown and draws an empty track rather than pretending 0.
 */
@Composable
fun GrowthProgress(progress: Float?, modifier: Modifier = Modifier) {
    val colors = PlentyTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(BarShape)
            .background(colors.border)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress ?: 0f, 0f..1f) },
    ) {
        if (progress != null && progress > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(BarShape)
                    .background(colors.accent),
            )
        }
    }
}
