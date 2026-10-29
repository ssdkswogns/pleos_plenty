package app.plenty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.plenty.R
import app.plenty.core.content.Trait
import app.plenty.ui.ContentRes
import app.plenty.ui.theme.PlentyTheme

enum class TraitBadgeStyle { Default, Selected, Disabled, Prefix }

/**
 * Trait badge (design/assets/badges): 150x44 capsule, icon + text so meaning never depends on color.
 * [TraitBadgeStyle.Prefix] shows the name prefix ("물가의") instead of the label ("물가").
 * Not interactive; the icon is decorative because the text says the same.
 */
@Composable
fun TraitBadge(trait: Trait, style: TraitBadgeStyle = TraitBadgeStyle.Default, modifier: Modifier = Modifier) {
    val colors = PlentyTheme.colors
    val (container, content) = when (style) {
        TraitBadgeStyle.Default, TraitBadgeStyle.Prefix -> colors.surfaceTonal to colors.primary
        TraitBadgeStyle.Selected -> colors.primary to colors.onPrimary
        TraitBadgeStyle.Disabled -> colors.background to colors.textSecondary
    }
    Row(
        modifier = modifier
            .height(44.dp)
            .widthIn(min = 150.dp)
            .background(container, CircleShape)
            .then(if (style == TraitBadgeStyle.Disabled) Modifier.border(1.dp, colors.border, CircleShape) else Modifier)
            .padding(start = 15.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(ContentRes.traitIcon(trait.id)), null, tint = content, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (style == TraitBadgeStyle.Prefix) trait.namePrefix else trait.label,
            style = PlentyTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = content,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (style == TraitBadgeStyle.Selected) {
            Spacer(Modifier.width(12.dp))
            Icon(painterResource(R.drawable.ic_check), null, tint = content, modifier = Modifier.size(18.dp))
        }
    }
}
