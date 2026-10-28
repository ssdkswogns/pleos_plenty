package app.plenty.core.rules

import app.plenty.core.content.GameContent
import app.plenty.core.content.RegionId
import kotlin.random.Random

/** Everything needed to settle one finished journey, read from storage in the same transaction. */
data class SettlementInput(
    val startOdometerKm: Double?,
    val endOdometerKm: Double?,
    /** Fixed when the journey started. */
    val companionIds: List<Long>,
    /** Region confirmed at arrival; null when the location was missing or stale. */
    val arrivalRegion: RegionId?,
    val plants: Map<Long, PlantState>,
    val xpGrantedToday: Map<Long, Int>,
    val regionRewardedToday: Boolean,
    val regionVisitedBefore: Boolean,
)

sealed interface SettlementPlan {
    data class Settle(
        val distanceKm: Double,
        val growth: List<PlantGrowth>,
        val regionReward: RegionReward?,
    ) : SettlementPlan

    /** Odometer not readable yet: grant nothing, keep the journey open, settle again later. */
    data object Pending : SettlementPlan

    /** Odometer reset or implausible: close the journey without any reward (GAME_RULES.md §5). */
    data class Reject(val reason: JourneyDistance.Reason) : SettlementPlan
}

/**
 * Combines distance validation, growth and the region reward into one result. Pure: the caller
 * stores the result under the journey's reward key, so a repeated settlement never re-rolls.
 */
class SettlementPlanner(private val content: GameContent) {

    private val growth = GrowthEngine(content)
    private val rewards = RegionRewardRules(content)

    fun plan(input: SettlementInput, random: Random): SettlementPlan {
        val distance = JourneyDistance.between(input.startOdometerKm, input.endOdometerKm, content.distance)
        val region = input.arrivalRegion?.let(content::region)
        val outcome = growth.settle(
            journey = JourneyInput(distance, input.companionIds, arrivalTrait = region?.trait),
            plants = input.plants,
            xpGrantedToday = input.xpGrantedToday,
        )
        return when (outcome) {
            SettlementOutcome.Pending -> SettlementPlan.Pending
            is SettlementOutcome.Rejected -> SettlementPlan.Reject(outcome.reason)
            is SettlementOutcome.Settled -> SettlementPlan.Settle(
                distanceKm = (distance as JourneyDistance.Valid).km,
                growth = outcome.growth,
                regionReward = region?.let {
                    rewards.roll(it.id, input.regionRewardedToday, input.regionVisitedBefore, random)
                },
            )
        }
    }
}
