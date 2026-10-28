package app.plenty.core.rules

import app.plenty.core.content.DefaultContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialIds
import app.plenty.core.content.SpeciesIds
import app.plenty.core.content.TraitId
import app.plenty.core.content.TraitIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrowthEngineTest {

    private val engine = GrowthEngine(DefaultContent)

    private fun seed(id: Long = 1, xp: Int = 0) = PlantState(id = id, species = SpeciesIds.CLOUD_FERN, xp = xp)

    private fun journey(km: Double, vararg companions: Long, trait: TraitId? = TraitIds.WATERSIDE) =
        JourneyInput(JourneyDistance.Valid(km), companions.toList(), trait)

    private fun settleOne(
        plant: PlantState,
        km: Double,
        trait: TraitId? = TraitIds.WATERSIDE,
        grantedToday: Int = 0,
    ): PlantGrowth {
        val outcome = engine.settle(journey(km, plant.id, trait = trait), mapOf(plant.id to plant), mapOf(plant.id to grantedToday))
        return (outcome as SettlementOutcome.Settled).growth.single()
    }

    @Test
    fun `one xp per km and the fraction carries to the next journey`() {
        val first = settleOne(seed(), km = 12.5)
        assertEquals(12, first.xpGained)
        assertEquals(0.5, first.after.xpRemainder, 1e-9)

        val second = settleOne(first.after, km = 0.6)
        assertEquals(1, second.xpGained)
        assertEquals(13, second.after.xp)
        assertEquals(0.1, second.after.xpRemainder, 1e-9)
    }

    @Test
    fun `only companions fixed at departure grow`() {
        val a = seed(id = 1)
        val b = seed(id = 2)
        val outcome = engine.settle(journey(10.0, 1), mapOf(1L to a, 2L to b), emptyMap()) as SettlementOutcome.Settled
        assertEquals(listOf(1L), outcome.growth.map { it.after.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `more than three companions is rejected`() {
        engine.settle(journey(10.0, 1, 2, 3, 4), emptyMap(), emptyMap())
    }

    @Test
    fun `daily cap limits xp and drops the overflow and fraction`() {
        val capped = settleOne(seed(), km = 10.7, grantedToday = 45)
        assertEquals(5, capped.xpGained)
        assertEquals(0.0, capped.after.xpRemainder, 0.0)

        val full = settleOne(seed(), km = 0.4, grantedToday = 50)
        assertEquals(0, full.xpGained)
        assertEquals(0.0, full.after.xpRemainder, 0.0)
    }

    @Test
    fun `unknown distance stays pending and invalid distance is rejected`() {
        val plants = mapOf(1L to seed())
        assertEquals(
            SettlementOutcome.Pending,
            engine.settle(JourneyInput(JourneyDistance.Unknown, listOf(1), TraitIds.CITY), plants, emptyMap()),
        )
        assertEquals(
            SettlementOutcome.Rejected(JourneyDistance.Reason.COUNTER_DECREASED),
            engine.settle(
                JourneyInput(JourneyDistance.Invalid(JourneyDistance.Reason.COUNTER_DECREASED), listOf(1), TraitIds.CITY),
                plants,
                emptyMap(),
            ),
        )
    }

    @Test
    fun `reaching exactly 20 xp makes a sprout named by the seed stage trait`() {
        val growth = settleOne(seed(), km = 20.0, trait = TraitIds.HIGHLAND)
        assertEquals(GrowthStage.SPROUT, growth.after.stage)
        assertEquals(TraitIds.HIGHLAND, growth.after.sproutPrefix)
        assertEquals(listOf(GrowthStage.SPROUT), growth.reachedStages)
        assertTrue(growth.after.stageTraitPoints.isEmpty())
    }

    @Test
    fun `xp crossing a threshold is split between the old and new stage`() {
        val start = seed(xp = 15).copy(stageTraitPoints = mapOf(TraitIds.CITY to 15))
        val growth = settleOne(start, km = 10.0, trait = TraitIds.WATERSIDE)
        // 5 xp finished the seed stage (city 15 vs waterside 5), the other 5 start the sprout stage.
        assertEquals(TraitIds.CITY, growth.after.sproutPrefix)
        assertEquals(mapOf(TraitIds.WATERSIDE to 5), growth.after.stageTraitPoints)
        assertEquals(25, growth.after.xp)
    }

    @Test
    fun `tie goes to the earlier trait in the content list`() {
        assertEquals(
            TraitIds.WATERSIDE,
            engine.prefixFor(mapOf(TraitIds.HIGHLAND to 10, TraitIds.WATERSIDE to 10)),
        )
    }

    @Test
    fun `unknown location still grants xp but no trait, so the name has no prefix`() {
        val growth = settleOne(seed(), km = 25.0, trait = null)
        assertEquals(25, growth.xpGained)
        assertEquals(GrowthStage.SPROUT, growth.after.stage)
        assertNull(growth.after.sproutPrefix)
    }

    @Test
    fun `mature name comes from the sprout stage and can differ from the sprout name`() {
        val sprout = PlantState(
            id = 1,
            species = SpeciesIds.MOONBELL,
            stage = GrowthStage.SPROUT,
            xp = 90,
            stageTraitPoints = mapOf(TraitIds.CITY to 40),
            sproutPrefix = TraitIds.WATERSIDE,
        )
        val growth = settleOne(sprout, km = 10.0, trait = TraitIds.HIGHLAND)
        assertEquals(GrowthStage.MATURE, growth.after.stage)
        assertEquals(TraitIds.WATERSIDE, growth.after.sproutPrefix)
        assertEquals(TraitIds.CITY, growth.after.maturePrefix)
    }

    @Test
    fun `mature plants keep gaining xp without changing name or scoring traits`() {
        val mature = PlantState(
            id = 1,
            species = SpeciesIds.TIDE_LOTUS,
            stage = GrowthStage.MATURE,
            xp = 120,
            maturePrefix = TraitIds.CITY,
        )
        val growth = settleOne(mature, km = 30.0, trait = TraitIds.WATERSIDE)
        assertEquals(150, growth.after.xp)
        assertEquals(TraitIds.CITY, growth.after.maturePrefix)
        assertTrue(growth.after.stageTraitPoints.isEmpty())
        assertTrue(growth.reachedStages.isEmpty())
    }

    @Test
    fun `materials add points to their linked trait and can change the outcome`() {
        val plant = seed(xp = 10).copy(stageTraitPoints = mapOf(TraitIds.CITY to 10))
        val boosted = engine.applyMaterial(plant, MaterialIds.WIND_STONE, count = 3)
        assertEquals(15, boosted.stageTraitPoints[TraitIds.HIGHLAND])

        val growth = settleOne(boosted, km = 10.0, trait = TraitIds.CITY)
        // city 10 + 10 = 20 vs highland 15 -> city; without the 10 km it would have been highland.
        assertEquals(TraitIds.CITY, growth.after.sproutPrefix)
        assertEquals(TraitIds.HIGHLAND, engine.prefixFor(boosted.stageTraitPoints))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `materials cannot rename a mature plant`() {
        engine.applyMaterial(seed().copy(stage = GrowthStage.MATURE), MaterialIds.MOON_DEW, count = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `material count must be positive`() {
        engine.applyMaterial(seed(), MaterialIds.MOON_DEW, count = 0)
    }
}
