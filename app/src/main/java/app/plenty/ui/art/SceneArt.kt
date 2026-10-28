package app.plenty.ui.art

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import app.plenty.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Layered garden artwork (plenty-motion-kit). Soft layers that are drawn at roughly half size are
 * decoded at half resolution; mesh-warped layers stay Android [Bitmap]s for drawBitmapMesh.
 */
@Immutable
class SceneArt(
    val world: ImageBitmap,
    val cloud: ImageBitmap,
    val foreground: ImageBitmap,
    /** Child + cart without the cape, 1586x992 source; pot anchors unchanged. */
    val travelerBase: Bitmap,
    /** Detached cape, 1585x992 source, drawn behind the base. */
    val cape: Bitmap,
)

/** Null until the artwork has been decoded off the main thread. */
val LocalSceneArt = staticCompositionLocalOf<SceneArt?> { null }

/** Decoding ~10 MB of PNGs on the main thread froze the first frames for ~3 s, so do it in the background. */
@Composable
fun rememberSceneArt(): SceneArt? {
    val resources = LocalContext.current.resources
    return produceState<SceneArt?>(initialValue = null, resources) {
        value = withContext(Dispatchers.Default) {
            SceneArt(
                world = resources.decode(R.drawable.world_background, sampleSize = 1).asImageBitmap(),
                cloud = resources.decode(R.drawable.cloud_bank, sampleSize = 2).asImageBitmap(),
                foreground = resources.decode(R.drawable.foreground_garden, sampleSize = 2).asImageBitmap(),
                travelerBase = resources.decode(R.drawable.traveler_base, sampleSize = 1),
                cape = resources.decode(R.drawable.traveler_cape, sampleSize = 2),
            )
        }
    }.value
}

private fun Resources.decode(@DrawableRes id: Int, sampleSize: Int): Bitmap =
    BitmapFactory.decodeResource(this, id, BitmapFactory.Options().apply { inSampleSize = sampleSize })
