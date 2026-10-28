package app.plenty.core.rules

import app.plenty.core.content.DefaultContent
import app.plenty.core.content.MaterialIds
import app.plenty.core.content.RegionId
import app.plenty.core.content.SpeciesIds
import app.plenty.core.content.TraitIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class SettlementPlannerTest {

    private val planner = SettlementPlanner(DefaultContent)
    private val plant = PlantState(id = 7, species = SpeciesIds.TIDE_LOTUS)

    private fun input(
        start: Double? = 100.0,
        end: Double? = 130.0,
        region: RegionId? = RegionId("busan"),
        rewardedToday: Boolean = false,
    ) = SettlementInput(
        startOdometerKm = start,
        endOdometerKm = end,
        companionIds = listOf(plant.id),
        arrivalRegion = region,
        plants = mapOf(plant.id to plant),
        xpGrantedToday = emptyMap(),
        regionRewardedToday = rewardedToday,
        regionVisitedBefore = false,
    )

    @Test
    fun `valid journey grows companions with the arrival region trait and rolls the region reward`() {
        val plan = planner.plan(input(), Random(1)) as SettlementPlan.Settle
        assertEquals(30.0, plan.distanceKm, 1e-9)
        val growth = plan.growth.single()
        assertEquals(30, growth.xpGained)
        assertEquals(TraitIds.WATERSIDE, growth.after.sproutPrefix)
        assertEquals(MaterialIds.MOON_DEW, plan.regionReward?.material)
    }

    @Test
    fun `unknown arrival region still grows without trait or region reward`() {
        val plan = planner.plan(input(region = null), Random(1)) as SettlementPlan.Settle
        assertNull(plan.growth.single().after.sproutPrefix)
        assertNull(plan.regionReward)
    }

    @Test
    fun `region already paid today gives growth only`() {
        val plan = planner.plan(input(rewardedToday = true), Random(1)) as SettlementPlan.Settle
        assertEquals(30, plan.growth.single().xpGained)
        assertNull(plan.regionReward)
    }

    @Test
    fun `missing odometer is pending and a reset odometer is rejected`() {
        assertEquals(SettlementPlan.Pending, planner.plan(input(end = null), Random(1)))
        assertEquals(
            SettlementPlan.Reject(JourneyDistance.Reason.COUNTER_DECREASED),
            planner.plan(input(start = 500.0, end = 20.0), Random(1)),
        )
    }
}
