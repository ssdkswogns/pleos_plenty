package app.plenty.ui.art

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import app.plenty.R

private const val CART_SOURCE_WIDTH = 1586f
private const val CART_SOURCE_HEIGHT = 992f
private const val PLANT_WIDTH_RATIO = 0.18f
private const val PLANT_TOP_ADJUST_SOURCE_PX = 36f

/**
 * Pot anchors in traveler_cart.png source pixels (asset-manifest.json `cart.slots`).
 * Index = slot id - 1, which is also the draw order (back row first, then front row).
 */
private val SlotAnchors = listOf(
    250f to 615f,
    474f to 565f,
    656f to 520f,
    410f to 657f,
    616f to 604f,
    814f to 559f,
)

/**
 * The child and the 6-pot cart with plants composited onto their slot anchors (HANDOFF §7):
 * plantSize = cartWidth x 0.18, left = anchorX x scale - plantSize / 2,
 * top = anchorY x scale - plantSize + 36 x scale.
 */
@Composable
fun TravelerCart(pots: List<PlantArt?>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.aspectRatio(CART_SOURCE_WIDTH / CART_SOURCE_HEIGHT)) {
        val scale = maxWidth / CART_SOURCE_WIDTH
        val plantSize = maxWidth * PLANT_WIDTH_RATIO
        Image(
            painter = painterResource(R.drawable.traveler_cart),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        pots.take(SlotAnchors.size).forEachIndexed { index, art ->
            if (art == null) return@forEachIndexed
            val (anchorX, anchorY) = SlotAnchors[index]
            PlantImage(
                art = art,
                contentDescription = null,
                modifier = Modifier
                    .offset(
                        x = scale * anchorX - plantSize / 2,
                        y = scale * anchorY - plantSize + scale * PLANT_TOP_ADJUST_SOURCE_PX,
                    )
                    .size(plantSize),
            )
        }
    }
}
