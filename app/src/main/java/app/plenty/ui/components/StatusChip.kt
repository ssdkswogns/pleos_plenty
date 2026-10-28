package app.plenty.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.plenty.ui.theme.PlentyTheme

/**
 * Capsule used for StatusBadge (parked / charging) and the header ResourceChip in 03-home:
 * 44 tall, icon + visible text, never color alone. [accessibilityLabel] replaces the visible text
 * for screen readers when the number needs a unit (e.g. "달빛이슬 12개").
 */
@Composable
fun StatusChip(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = text,
) {
    val colors = PlentyTheme.colors
    Row(
        modifier = modifier
            .height(44.dp)
            .widthIn(min = 108.dp)
            .background(colors.surface.copy(alpha = 0.96f), CircleShape)
            .clearAndSetSemantics { contentDescription = accessibilityLabel }
            .padding(start = 14.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = PlentyTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = colors.primary,
        )
    }
}
