package app.plenty.ui.art

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.plenty.R
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.SpeciesId
import app.plenty.core.content.SpeciesIds

/**
 * Plant artwork in plant_atlas.png. Rows are 440 / 410 / 404 px tall, not equal squares, so each
 * region comes from asset-manifest.json `atlas.rects`; never slice the atlas into 418x418 cells.
 */
enum class PlantArt(
    @StringRes val speciesName: Int,
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
) {
    MoonbellSeed(R.string.plant_moonbell, 0, 0, 418, 440),
    MoonbellSprout(R.string.plant_moonbell, 418, 0, 418, 440),
    MoonbellMature(R.string.plant_moonbell, 836, 0, 418, 440),
    CloudFernSeed(R.string.plant_cloud_fern, 0, 440, 418, 410),
    CloudFernSprout(R.string.plant_cloud_fern, 418, 440, 418, 410),
    CloudFernMature(R.string.plant_cloud_fern, 836, 440, 418, 410),
    TideLotusSeed(R.string.plant_tide_lotus, 0, 850, 418, 404),
    TideLotusSprout(R.string.plant_tide_lotus, 418, 850, 418, 404),
    TideLotusMature(R.string.plant_tide_lotus, 836, 850, 418, 404);

    val srcOffset: IntOffset get() = IntOffset(x, y)
    val srcSize: IntSize get() = IntSize(width, height)

    companion object {
        /** Art for a species at a stage; SEED is the 말랑 artwork. Regional traits never change the art. */
        fun of(species: SpeciesId, stage: GrowthStage): PlantArt = when (species) {
            SpeciesIds.MOONBELL -> pick(stage, MoonbellSeed, MoonbellSprout, MoonbellMature)
            SpeciesIds.CLOUD_FERN -> pick(stage, CloudFernSeed, CloudFernSprout, CloudFernMature)
            SpeciesIds.TIDE_LOTUS -> pick(stage, TideLotusSeed, TideLotusSprout, TideLotusMature)
            else -> error("no art for $species")
        }

        private fun pick(stage: GrowthStage, seed: PlantArt, sprout: PlantArt, mature: PlantArt) = when (stage) {
            GrowthStage.SEED -> seed
            GrowthStage.SPROUT -> sprout
            GrowthStage.MATURE -> mature
        }
    }
}

/** The decoded atlas, provided once at the app root so every plant shares one ~6 MB bitmap. */
val LocalPlantAtlas = staticCompositionLocalOf<ImageBitmap> {
    error("LocalPlantAtlas not provided")
}

/** Draws one plant region, letterboxed (SVG "meet") so leaves and pot are never cropped. */
@Composable
fun PlantImage(art: PlantArt, contentDescription: String?, modifier: Modifier = Modifier) {
    val atlas = LocalPlantAtlas.current
    val painter = remember(atlas, art) { BitmapPainter(atlas, art.srcOffset, art.srcSize) }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
