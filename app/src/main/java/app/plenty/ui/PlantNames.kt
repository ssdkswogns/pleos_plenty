package app.plenty.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.plenty.R
import app.plenty.core.content.DefaultContent
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.SpeciesId
import app.plenty.core.content.SpeciesIds
import app.plenty.core.content.TraitId
import app.plenty.core.rules.PlantState

@StringRes
fun speciesNameRes(species: SpeciesId): Int = when (species) {
    SpeciesIds.MOONBELL -> R.string.plant_moonbell
    SpeciesIds.CLOUD_FERN -> R.string.plant_cloud_fern
    SpeciesIds.TIDE_LOTUS -> R.string.plant_tide_lotus
    else -> error("no name for $species")
}

@StringRes
fun stageNameRes(stage: GrowthStage): Int = when (stage) {
    GrowthStage.SEED -> R.string.stage_seed
    GrowthStage.SPROUT -> R.string.stage_sprout
    GrowthStage.MATURE -> R.string.stage_mature
}

/**
 * "물결연꽃 말랑", "물가의 구름고사리 새싹", "산자락의 구름고사리" (GAME_RULES.md §6).
 * The prefix comes from the trait that decided the current stage; none when no trait was known.
 */
@Composable
fun plantDisplayName(plant: PlantState, content: GameContent = DefaultContent): String {
    val species = stringResource(speciesNameRes(plant.species))
    return when (plant.stage) {
        GrowthStage.SEED -> stringResource(R.string.mallang_name, species)
        GrowthStage.SPROUT -> stringResource(R.string.sprout_name, prefixed(plant.sproutPrefix, species, content))
        GrowthStage.MATURE -> prefixed(plant.maturePrefix, species, content)
    }
}

private fun prefixed(trait: TraitId?, species: String, content: GameContent): String {
    val prefix = content.traits.firstOrNull { it.id == trait }?.namePrefix
    return if (prefix == null) species else "$prefix $species"
}
