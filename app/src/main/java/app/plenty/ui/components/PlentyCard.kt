package app.plenty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.plenty.ui.theme.PlentyRadius
import app.plenty.ui.theme.PlentyTheme

private val CardShape = RoundedCornerShape(PlentyRadius.panel)

/** White panel with the soft navy shadow and gray-blue hairline used by every card in the kit. */
@Composable
fun PlentyCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = PlentyTheme.colors
    // Caller modifiers (size, click) sit inside the clip so press feedback keeps the rounded corners.
    Box(
        modifier = Modifier
            .shadow(12.dp, CardShape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(CardShape)
            .then(modifier)
            .background(colors.surface.copy(alpha = 0.96f))
            .border(1.dp, colors.border, CardShape),
        content = content,
    )
}
