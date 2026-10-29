package app.plenty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.ui.theme.PlentyRadius
import app.plenty.ui.theme.PlentyTheme

private val BackShape = RoundedCornerShape(PlentyRadius.button)

/**
 * Sticky header of detail/confirm screens (S02): 64x64 back button, title 20 to its right,
 * subtitle under both. Screens never rely on a system back button, which Pleos does not show.
 * [backTo] names the destination for screen readers ("뒤로, 정원").
 */
@Composable
fun DetailHeader(
    title: String,
    subtitle: String,
    backTo: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(BackShape)
                    .background(colors.surface.copy(alpha = 0.96f))
                    .border(1.dp, colors.border, BackShape)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back_to, backTo),
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(20.dp))
            Text(
                text = title,
                style = PlentyTheme.typography.title,
                color = colors.text,
                maxLines = 2,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(subtitle, style = PlentyTheme.typography.body.copy(fontSize = 22.sp, lineHeight = 30.sp), color = colors.textSecondary)
    }
}
