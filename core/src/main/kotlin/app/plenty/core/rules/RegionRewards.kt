package app.plenty.core.rules

import app.plenty.core.content.GameContent
import app.plenty.core.content.MaterialId
import app.plenty.core.content.RegionId
import app.plenty.core.content.SpeciesId
import kotlin.random.Random

data class RegionReward(
    val region: RegionId,
    val material: MaterialId,
    val materialCount: Int,
    /** A 말랑 of this species goes to the bag; null = no 말랑 this time. */
    val seed: SpeciesId?,
)

/**
 * Region visit rewards (GAME_RULES.md §4). [Random] is injected so the caller can draw once,
 * store the result under its reward key, and never re-draw on a repeated settlement (T05).
 */
class RegionRewardRules(private val content: GameContent) {

    /** Null when this region already paid out today. */
    fun roll(region: RegionId, rewardedToday: Boolean, visitedBefore: Boolean, random: Random): RegionReward? {
        if (rewardedToday) return null
        val definition = requireNotNull(content.region(region)) { "unknown region $region" }
        val givesSeed = !visitedBefore || random.nextDouble() < content.rewards.repeatSeedChance
        return RegionReward(
            region = definition.id,
            material = definition.material,
            materialCount = content.rewards.materialPerVisit,
            seed = if (givesSeed) content.species[random.nextInt(content.species.size)] else null,
        )
    }
}
