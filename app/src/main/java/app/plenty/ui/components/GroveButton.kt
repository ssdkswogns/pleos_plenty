package app.plenty.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.plenty.R
import app.plenty.ui.theme.PlentyMotion
import app.plenty.ui.theme.PlentyRadius
import app.plenty.ui.theme.PlentyTheme
import app.plenty.ui.theme.PlentyTouch

enum class GroveButtonStyle { Primary, Secondary, Tonal }

private val ButtonShape = RoundedCornerShape(PlentyRadius.button)

/**
 * GroveButton (components-and-motion.md): 64 tall, 20 corners. Pressed = 8% dark veil over 120 ms;
 * disabled = gray-blue surface with muted text. The trailing arrow sits 19 from the end (03-home).
 */
@Composable
fun GroveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GroveButtonStyle = GroveButtonStyle.Primary,
    enabled: Boolean = true,
    @DrawableRes trailingIcon: Int? = R.drawable.ic_arrow,
) {
    val colors = PlentyTheme.colors
    val container: Color
    val content: Color
    var border: BorderStroke? = null
    when {
        !enabled -> {
            container = colors.disabledSurface
            content = colors.disabledText
        }
        style == GroveButtonStyle.Primary -> {
            container = colors.primary
            content = colors.onPrimary
        }
        style == GroveButtonStyle.Secondary -> {
            container = colors.surface
            content = colors.primary
            border = BorderStroke(1.dp, colors.border)
        }
        else -> {
            container = colors.surfaceTonal
            content = colors.primary
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val veil by animateColorAsState(
        targetValue = if (pressed) Color.Black.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(PlentyMotion.PRESS_MS),
        label = "pressVeil",
    )
    Box(
        modifier = modifier
            .height(PlentyTouch.primaryHeight)
            .clip(ButtonShape)
            .background(container)
            .background(veil)
            .then(if (border != null) Modifier.border(border, ButtonShape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = PlentyTheme.typography.button,
            color = content,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 56.dp),
        )
        if (trailingIcon != null) {
            Icon(
                painter = painterResource(trailingIcon),
                contentDescription = null,
                tint = content,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 19.dp)
                    .size(26.dp),
            )
        }
    }
}
