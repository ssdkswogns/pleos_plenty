package app.plenty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.theme.PlentyTheme

private val SlotShape = RoundedCornerShape(18.dp)

/**
 * One of the 6 numbered pot slots (PotSlot in components-and-motion.md). Empty slots show "+".
 * The slot number is the same id as the cart anchor, so slot 1 here is slot 1 on the cart.
 */
@Composable
fun PotSlot(
    number: Int,
    plant: PlantArt?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val colors = PlentyTheme.colors
    val description = if (plant != null) {
        stringResource(R.string.pot_filled_a11y, number, stringResource(plant.speciesName))
    } else {
        stringResource(R.string.pot_empty_a11y, number)
    }
    Box(
        modifier = modifier
            .size(width = 112.dp, height = 102.dp)
            .clip(SlotShape)
            .background(colors.background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accent else colors.border,
                shape = SlotShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        if (plant != null) {
            PlantImage(
                art = plant,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .size(width = 80.dp, height = 74.dp),
            )
            Text(
                text = number.toString(),
                style = PlentyTheme.typography.caption.copy(fontSize = 14.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 12.dp)
                    .clearAndSetSemantics {},
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp),
            )
        }
    }
}
