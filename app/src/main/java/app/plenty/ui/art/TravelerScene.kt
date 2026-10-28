package app.plenty.ui.art

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.plenty.ui.motion.HomeMotion
import app.plenty.ui.theme.PlentyMotion
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CART_W = 1586f
private const val CART_H = 992f
private const val PLANT_RATIO = 0.18f
private const val PLANT_TOP_ADJUST = 36f
private const val TAU = (2 * PI).toFloat()

// Cape placement inside the cart source space (plenty-motion-kit README).
private const val CAPE_X = 930f
private const val CAPE_Y = 218f
private const val CAPE_SCALE = 0.4f
private const val CAPE_W = 1585f
private const val CAPE_H = 992f
private const val CAPE_ROOT_X = 145f

/** Pot anchors in cart source pixels (asset-manifest.json); index = slot id - 1 = draw order. */
private val SlotAnchors = listOf(
    250f to 615f,
    474f to 565f,
    656f to 520f,
    410f to 657f,
    616f to 604f,
    814f to 559f,
)

/**
 * The child, detached cape and 6-pot cart with planted pots. Cart, child and plants share one
 * parent transform so pots never slide off their anchors (plenty-motion-kit README).
 * Tap a plant to make it hop; tap anywhere else for a short gust.
 */
@Composable
fun TravelerScene(pots: List<PlantArt?>, motion: HomeMotion, modifier: Modifier = Modifier) {
    val art = LocalSceneArt.current
    val atlas = LocalPlantAtlas.current
    val capeMesh = remember { MeshGrid(cols = 20, rows = 12, width = CAPE_W, height = CAPE_H) }
    val bodyMesh = remember { MeshGrid(cols = 20, rows = 16, width = CART_W, height = CART_H) }
    val paint = remember { Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG) }
    val currentPots by rememberUpdatedState(pots)
    val reveal by animateFloatAsState(if (art != null) 1f else 0f, tween(PlentyMotion.PANEL_MS), label = "actorReveal")

    Canvas(
        modifier
            .aspectRatio(CART_W / CART_H)
            .graphicsLayer { alpha = reveal }
            .pointerInput(motion) {
                detectTapGestures { tap ->
                    val scale = size.width / CART_W
                    val slot = plantAt(currentPots, tap.x / scale, tap.y / scale)
                    if (slot != null) motion.wiggle(slot) else motion.gust()
                }
            },
    ) {
        if (art == null) return@Canvas
        val scale = size.width / CART_W
        val still = !motion.running
        val t = motion.time
        val gust = motion.gustLevel
        val wind = 1f + 0.35f * gust
        // Whole actor: 2 units of bob (3 s) and about +-0.17 degrees of tilt (6 s).
        val bob = if (still) 0f else 2.dp.toPx() * sin(TAU * t / 3f)
        val tilt = if (still) 0f else radToDeg(0.003f * sin(TAU * t / 6f))

        withTransform({
            translate(top = bob)
            rotate(tilt, pivot = Offset(780f * scale, 650f * scale))
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                // Cape first: its root hides under the shoulder of the base.
                native.save()
                native.translate(CAPE_X, CAPE_Y)
                native.scale(CAPE_SCALE, CAPE_SCALE)
                capeMesh.fill(still) { x, y -> capeWarp(x, y, t, wind) }
                native.drawBitmapMesh(art.cape, capeMesh.cols, capeMesh.rows, capeMesh.verts, 0, null, 0, paint)
                native.restore()

                bodyMesh.fill(still) { x, y -> bodyWarp(x, y, t) }
                native.drawBitmapMesh(art.travelerBase, bodyMesh.cols, bodyMesh.rows, bodyMesh.verts, 0, null, 0, paint)
            }
            pots.take(SlotAnchors.size).forEachIndexed { slot, plant ->
                if (plant != null) {
                    drawPlant(atlas, plant, slot, t, still, gust, motion.wiggleLevel(slot))
                }
            }
        }
    }
}

/** Cloth on the shoulder: weight 0 at the root, about 57 source px at the tip, tip lags behind. */
private fun capeWarp(x: Float, y: Float, t: Float, wind: Float): Offset {
    val u = smooth((x - CAPE_ROOT_X) / (CAPE_W - CAPE_ROOT_X))
    val theta = TAU * t / 1.5f - TAU * x / CAPE_W * 0.9f
    return Offset(
        x + 8f * u * sin(theta + 0.7f) * wind,
        y + (44f * u * sin(theta) + 13f * u * u * sin(2f * theta + y / CAPE_H)) * wind,
    )
}

/** Head sway (6 s, +-7 / +-4 px) and shoulder breath (3 s, 2 px); hands and cart stay locked. */
private fun bodyWarp(x: Float, y: Float, t: Float): Offset {
    val head = exp(-sq((x - 940f) / 205f)) * smooth((365f - y) / 150f)
    val shoulder = exp(-sq((x - 980f) / 180f)) * exp(-sq((y - 315f) / 110f))
    val handLock = 1f - smooth((y - 325f) / 35f)
    return Offset(
        x + 7f * head * sin(TAU * t / 6f),
        y + 4f * head * sin(TAU * t / 6f + 0.8f) + 2f * shoulder * handLock * sin(TAU * t / 3f),
    )
}

/** Plant pivots on its pot bottom: about +-0.52 degrees, phase per slot, stronger in a gust. */
private fun DrawScope.drawPlant(
    atlas: ImageBitmap,
    plant: PlantArt,
    slot: Int,
    t: Float,
    still: Boolean,
    gust: Float,
    hop: Float,
) {
    val (anchorX, anchorY) = SlotAnchors[slot]
    val box = CART_W * PLANT_RATIO
    val fit = min(box / plant.srcSize.width, box / plant.srcSize.height)
    val width = plant.srcSize.width * fit
    val height = plant.srcSize.height * fit
    val sway = if (still) 0f else radToDeg(0.009f * (1f + gust) * sin(TAU * t / 3f + slot * 0.7f))
    val hopTilt = 6f * sin(hop * 3f * PI.toFloat()) * (1f - hop)
    val hopScale = 1f + 0.08f * sin(hop * PI.toFloat())
    withTransform({
        translate(anchorX, anchorY + PLANT_TOP_ADJUST)
        rotate(sway + hopTilt, pivot = Offset.Zero)
        scale(hopScale, hopScale, pivot = Offset.Zero)
    }) {
        drawImage(
            image = atlas,
            srcOffset = plant.srcOffset,
            srcSize = plant.srcSize,
            dstOffset = IntOffset((-width / 2).roundToInt(), (-box + (box - height) / 2).roundToInt()),
            dstSize = IntSize(width.roundToInt(), height.roundToInt()),
        )
    }
}

/** Front-most planted pot under a point in cart source space, as a 0-based slot. */
private fun plantAt(pots: List<PlantArt?>, x: Float, y: Float): Int? {
    val box = CART_W * PLANT_RATIO
    for (slot in pots.indices.reversed()) {
        if (slot >= SlotAnchors.size || pots[slot] == null) continue
        val (anchorX, anchorY) = SlotAnchors[slot]
        val bottom = anchorY + PLANT_TOP_ADJUST
        if (x in (anchorX - box / 2)..(anchorX + box / 2) && y in (bottom - box)..bottom) return slot
    }
    return null
}

/** A drawBitmapMesh grid. Vertices are shared between cells, so no seams appear between triangles. */
private class MeshGrid(val cols: Int, val rows: Int, val width: Float, val height: Float) {
    val verts = FloatArray((cols + 1) * (rows + 1) * 2)

    inline fun fill(still: Boolean, warp: (x: Float, y: Float) -> Offset) {
        var i = 0
        for (row in 0..rows) {
            val y = height * row / rows
            for (col in 0..cols) {
                val x = width * col / cols
                val p = if (still) Offset(x, y) else warp(x, y)
                verts[i++] = p.x
                verts[i++] = p.y
            }
        }
    }
}

private fun smooth(v: Float): Float {
    val c = v.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun sq(v: Float) = v * v

private fun radToDeg(rad: Float) = rad * 180f / PI.toFloat()
