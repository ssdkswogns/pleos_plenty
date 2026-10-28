package app.plenty.ui.art

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.plenty.R
import app.plenty.ui.theme.PlentyTheme

/**
 * Sky-and-hills background with the left-to-right light veil from 03-home, so text never sits
 * directly on bright artwork (components-and-motion.md layout rules).
 */
@Composable
fun WorldBackdrop(modifier: Modifier = Modifier) {
    val veil = PlentyTheme.colors.background
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.world_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to veil.copy(alpha = 0.98f),
                        0.37f to veil.copy(alpha = 0.82f),
                        0.72f to veil.copy(alpha = 0.08f),
                    ),
                ),
        )
    }
}
