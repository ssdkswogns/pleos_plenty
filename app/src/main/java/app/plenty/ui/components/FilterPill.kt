package app.plenty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.plenty.ui.theme.PlentyTheme

/** Tab capsule from 14-inventory: navy when selected, white otherwise; 44 tall, at least 100 wide. */
@Composable
fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PlentyTheme.colors
    Box(
        modifier = modifier
            .height(44.dp)
            .widthIn(min = 100.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.surface.copy(alpha = 0.96f))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = PlentyTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = if (selected) colors.onPrimary else colors.primary,
        )
    }
}
