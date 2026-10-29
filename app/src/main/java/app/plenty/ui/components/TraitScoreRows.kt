package app.plenty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.core.content.TraitId
import app.plenty.ui.ContentRes
import app.plenty.ui.theme.PlentyPalette
import app.plenty.ui.theme.PlentyTheme
import kotlin.math.ceil
import kotlin.math.max

private val BarShape = RoundedCornerShape(5.dp)

/**
 * The three trait scores of the current stage as integer points with comparison bars
 * (INTERACTIONS.md §10). All bars share one scale: max(10, the highest score incl. preview rounded
 * up to 10). A full bar does not mean anything is unlocked. With [preview], rows read "12 → 14점"
 * and the added part is drawn darker.
 */
@Composable
fun TraitScoreRows(
    content: GameContent,
    points: Map<TraitId, Int>,
    modifier: Modifier = Modifier,
    preview: Map<TraitId, Int>? = null,
) {
    val highest = content.traits.maxOf { max(points[it.id] ?: 0, preview?.get(it.id) ?: 0) }
    val scale = max(10, ceil(highest / 10.0).toInt() * 10)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {
        content.traits.forEach { trait ->
            val before = points[trait.id] ?: 0
            val after = preview?.get(trait.id) ?: before
            val value = if (preview == null) {
                stringResource(R.string.points, before)
            } else {
                stringResource(R.string.points_change, before, after)
            }
            Column(Modifier.clearAndSetSemantics { contentDescription = "${trait.label} $value" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(ContentRes.traitIcon(trait.id)), null, tint = PlentyTheme.colors.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(trait.label, style = PlentyTheme.typography.label.copy(fontWeight = FontWeight.Medium), color = PlentyTheme.colors.text, modifier = Modifier.weight(1f))
                    Text(value, style = PlentyTheme.typography.label.copy(fontWeight = FontWeight.SemiBold), color = PlentyTheme.colors.primary)
                }
                Spacer(Modifier.height(14.dp))
                ScoreBar(before = before, after = after, scale = scale)
            }
        }
    }
}

@Composable
private fun ScoreBar(before: Int, after: Int, scale: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(BarShape)
            .background(PlentyTheme.colors.border),
    ) {
        if (before > 0) Segment(start = 0f, fraction = before.toFloat() / scale, color = PlentyPalette.Sky)
        if (after > before) Segment(start = before.toFloat() / scale, fraction = (after - before).toFloat() / scale, color = PlentyTheme.colors.accent)
    }
}

/** A bar piece from [start] (0..1) spanning [fraction] of the track width. */
@Composable
private fun Segment(start: Float, fraction: Float, color: Color) {
    Layout(
        content = { Box(Modifier.fillMaxHeight().clip(BarShape).background(color)) },
    ) { measurables, constraints ->
        val width = (constraints.maxWidth * fraction.coerceIn(0f, 1f)).toInt()
        val x = (constraints.maxWidth * start.coerceIn(0f, 1f)).toInt()
        val placeable = measurables.single().measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(x, 0) }
    }
}
