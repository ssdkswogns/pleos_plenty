package app.plenty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.ui.theme.PlentyTheme

private val StepShape = RoundedCornerShape(20.dp)

/** − value + with 64x64 buttons (INTERACTIONS.md §12); a bound disables its button. */
@Composable
fun QuantityStepper(value: Int, range: IntRange, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StepButton(R.drawable.ic_minus, stringResource(R.string.decrease), enabled = value > range.first) { onChange(value - 1) }
        Text(
            text = value.toString(),
            style = PlentyTheme.typography.title.copy(fontSize = 30.sp),
            color = PlentyTheme.colors.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        StepButton(R.drawable.ic_plus, stringResource(R.string.increase), enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: Int, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = PlentyTheme.colors
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(StepShape)
            .background(if (enabled) colors.surface else colors.background)
            .border(1.dp, colors.border, StepShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), label, tint = if (enabled) colors.primary else colors.border, modifier = Modifier.size(24.dp))
    }
}
