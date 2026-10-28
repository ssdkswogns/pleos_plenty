package app.plenty.ui.art

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.plenty.ui.motion.HomeMotion
import app.plenty.ui.theme.PlentyMotion
import app.plenty.ui.theme.PlentyTheme
import kotlin.math.PI
import kotlin.math.sin

private const val TAU = (2 * PI).toFloat()

/** Positions below are in the 1440x900 design canvas of plenty-motion-kit/preview/scene.js. */
private const val DESIGN_W = 1440f
private const val DESIGN_H = 900f

/**
 * Garden sky behind all UI: drifting background, clouds, low mist, the light veil, a few motes and
 * the swaying foreground plants. Bitmap layers are drawn once and only their layer transform changes
 * per frame (graphicsLayer); everything is still when [motion] is not running.
 */
@Composable
fun WorldBackdrop(motion: HomeMotion, modifier: Modifier = Modifier) {
    val art = LocalSceneArt.current
    val veilColor = PlentyTheme.colors.background
    // Lighter than the original 03-home veil so more of the scenery shows (motion kit).
    val veil = remember(veilColor) {
        Brush.horizontalGradient(
            0f to veilColor.copy(alpha = 0.94f),
            0.28f to veilColor.copy(alpha = 0.70f),
            0.52f to veilColor.copy(alpha = 0.18f),
            0.78f to veilColor.copy(alpha = 0.02f),
        )
    }
    val reveal by animateFloatAsState(if (art != null) 1f else 0f, tween(PlentyMotion.PANEL_MS), label = "sceneReveal")

    BoxWithConstraints(modifier.graphicsLayer { alpha = reveal }) {
        val sx = maxWidth.value / DESIGN_W
        val sy = maxHeight.value / DESIGN_H
        val d = { design: Float -> (design * sx).dp }

        if (art != null) {
            // Background: cover with 4% overscan, drift x+-12 / y+-5 over 24 s.
            Image(
                bitmap = art.world,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.04f
                        scaleY = 1.04f
                        translationX = motion.wave(12f * sx, 24f, 0f).dp.toPx()
                        translationY = motion.wave(5f * sy, 24f, 0.6f).dp.toPx()
                    },
            )
            // Upper cloud bank and low mist reuse one texture.
            Layer(art.cloud, x = d(515f), y = (74f * sy).dp, width = d(1120f), height = d(414f), alpha = 0.27f) {
                translationX = motion.wave(42f * sx, 24f, 0.5f).dp.toPx()
            }
            Layer(art.cloud, x = d(285f), y = (410f * sy).dp, width = d(1260f), height = d(200f), alpha = 0.13f) {
                translationX = motion.wave(22f * sx, 12f, 2f).dp.toPx()
            }
        }

        Box(Modifier.fillMaxSize().background(veil))

        // Up to nine faint motes on the scenery side; the only per-frame raster work here.
        Canvas(Modifier.fillMaxSize()) {
            val t = if (motion.running) motion.time else 0f
            val px = size.width / DESIGN_W
            val py = size.height / DESIGN_H
            for (i in 0 until 9) {
                val q = t / 12f + i / 9f
                val x = 770f + (i * 79f + q * 38f) % 580f
                val y = 240f + ((i * 83f - q * 36f) % 380f + 380f) % 380f
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f + 0.17f * (0.5f + 0.5f * sin(TAU * q))),
                    radius = (1.5f + i % 3) * px,
                    center = Offset(x * px, y * py),
                )
            }
        }

        if (art != null) {
            // Foreground plants pivot on their bottom-right corner (6 s, about +-0.46 degrees).
            val pivotX = 1450f * sx
            val pivotY = 815f * sy
            Layer(
                art.foreground,
                x = (pivotX - 760f * sx).dp,
                y = (pivotY - 476f * sx).dp,
                width = d(760f),
                height = d(476f),
                alpha = 0.72f,
            ) {
                transformOrigin = TransformOrigin(1f, 1f)
                rotationZ = motion.wave(0.008f, 6f, 1.2f) * 180f / PI.toFloat()
            }
        }
    }
}

/**
 * One bitmap layer at a fixed design position; [transform] runs per frame without redrawing pixels.
 * [alpha] goes on the draw call, not the layer: a translucent graphicsLayer renders offscreen every
 * frame, which halved the frame rate on the emulator.
 */
@Composable
private fun Layer(
    image: ImageBitmap,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    alpha: Float = 1f,
    transform: GraphicsLayerScope.() -> Unit,
) {
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        alpha = alpha,
        modifier = Modifier
            .offset(x, y)
            .size(width, height)
            .graphicsLayer(transform),
    )
}

/** Sine offset for ambient drift; 0 when motion is stopped. */
private fun HomeMotion.wave(amplitude: Float, period: Float, phase: Float): Float =
    if (running) amplitude * sin(TAU * time / period + phase) else 0f
