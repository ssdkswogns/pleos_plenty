package app.plenty.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.plenty.R
import app.plenty.core.content.DefaultContent
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialId
import app.plenty.core.content.MaterialIds
import app.plenty.core.content.SpeciesId
import app.plenty.core.content.SpeciesIds
import app.plenty.core.content.Trait
import app.plenty.core.content.TraitId
import app.plenty.core.content.TraitIds
import app.plenty.core.rules.GrowthEngine
import app.plenty.core.rules.PlantState

/** Display resources for content IDs (design/spec/content-v1.json). */
object ContentRes {
    @StringRes
    fun speciesName(species: SpeciesId): Int = when (species) {
        SpeciesIds.MOONBELL -> R.string.plant_moonbell
        SpeciesIds.CLOUD_FERN -> R.string.plant_cloud_fern
        SpeciesIds.TIDE_LOTUS -> R.string.plant_tide_lotus
        else -> error("no name for $species")
    }

    @StringRes
    fun speciesTagline(species: SpeciesId): Int = when (species) {
        SpeciesIds.MOONBELL -> R.string.tagline_moonbell
        SpeciesIds.CLOUD_FERN -> R.string.tagline_cloud_fern
        SpeciesIds.TIDE_LOTUS -> R.string.tagline_tide_lotus
        else -> error("no tagline for $species")
    }

    @StringRes
    fun stageName(stage: GrowthStage): Int = when (stage) {
        GrowthStage.SEED -> R.string.stage_seed
        GrowthStage.SPROUT -> R.string.stage_sprout
        GrowthStage.MATURE -> R.string.stage_mature
    }

    @StringRes
    fun materialName(material: MaterialId): Int = when (material) {
        MaterialIds.MOON_DEW -> R.string.material_moon_dew
        MaterialIds.WIND_STONE -> R.string.material_wind_stone
        MaterialIds.CLOUD_LIGHT -> R.string.material_cloud_light
        else -> error("no name for $material")
    }

    @StringRes
    fun materialFlavor(material: MaterialId): Int = when (material) {
        MaterialIds.MOON_DEW -> R.string.flavor_moon_dew
        MaterialIds.WIND_STONE -> R.string.flavor_wind_stone
        MaterialIds.CLOUD_LIGHT -> R.string.flavor_cloud_light
        else -> error("no flavor for $material")
    }

    @DrawableRes
    fun materialIcon(material: MaterialId): Int = when (material) {
        MaterialIds.MOON_DEW -> R.drawable.ic_drop
        MaterialIds.WIND_STONE -> R.drawable.ic_stone
        MaterialIds.CLOUD_LIGHT -> R.drawable.ic_spark
        else -> error("no icon for $material")
    }

    @DrawableRes
    fun traitIcon(trait: TraitId): Int = when (trait) {
        TraitIds.WATERSIDE -> R.drawable.ic_trait_waterside
        TraitIds.HIGHLAND -> R.drawable.ic_trait_highland
        TraitIds.CITY -> R.drawable.ic_trait_city
        else -> error("no icon for $trait")
    }
}

/** Materials in content order: each boosts exactly one trait (DECISIONS.md §1). */
fun GameContent.materialsInOrder(): List<MaterialId> = traits.mapNotNull { it.boostMaterial }

fun GameContent.trait(id: TraitId?): Trait? = traits.firstOrNull { it.id == id }

/**
 * Current name from the stored prefixes (DECISIONS.md §2): "물결연꽃 말랑", "물가의 달빛종 새싹",
 * "달빛종 새싹", "산자락의 달빛종". Never recomputed from current scores.
 */
@Composable
fun plantDisplayName(plant: PlantState, content: GameContent = DefaultContent): String {
    val prefix = when (plant.stage) {
        GrowthStage.SEED -> null
        GrowthStage.SPROUT -> plant.sproutPrefix
        GrowthStage.MATURE -> plant.maturePrefix
    }
    return stageName(plant.species, plant.stage, content.trait(prefix))
}

/**
 * "What it would be called if it reached the next stage right now": next stage + the trait that
 * wins the current points with the engine's tie rule. Not a forecast of future journeys.
 */
@Composable
fun nextStageName(plant: PlantState, points: Map<TraitId, Int> = plant.stageTraitPoints, content: GameContent = DefaultContent): String? {
    val next = when (plant.stage) {
        GrowthStage.SEED -> GrowthStage.SPROUT
        GrowthStage.SPROUT -> GrowthStage.MATURE
        GrowthStage.MATURE -> return null
    }
    return stageName(plant.species, next, content.trait(GrowthEngine(content).prefixFor(points)))
}

@Composable
private fun stageName(species: SpeciesId, stage: GrowthStage, prefix: Trait?): String {
    val name = stringResource(ContentRes.speciesName(species))
    val prefixed = if (prefix == null) name else "${prefix.namePrefix} $name"
    return when (stage) {
        GrowthStage.SEED -> stringResource(R.string.mallang_name, name)
        GrowthStage.SPROUT -> stringResource(R.string.sprout_name, prefixed)
        GrowthStage.MATURE -> prefixed
    }
}

/** Cumulative XP of the next stage, or null for mature plants (thresholds from GrowthConfig). */
fun GameContent.nextStageXp(stage: GrowthStage): Int? = when (stage) {
    GrowthStage.SEED -> growth.sproutXp
    GrowthStage.SPROUT -> growth.matureXp
    GrowthStage.MATURE -> null
}

/** Two-digit pot label used across 10/11: 1 -> "01". */
fun potLabel(slot: Int): String = slot.toString().padStart(2, '0')
