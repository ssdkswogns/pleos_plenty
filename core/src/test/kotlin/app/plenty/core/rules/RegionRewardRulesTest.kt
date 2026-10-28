package app.plenty.core.rules

import app.plenty.core.content.DefaultContent
import app.plenty.core.content.MaterialIds
import app.plenty.core.content.RegionId
import app.plenty.core.content.SpeciesIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class RegionRewardRulesTest {

    private val rules = RegionRewardRules(DefaultContent)
    private val gangneung = RegionId("gangneung")

    /** Deterministic draws: [double] for the 30% check, [index] for the species pick. */
    private class FixedRandom(private val double: Double, private val index: Int) : Random() {
        override fun nextBits(bitCount: Int) = 0
        override fun nextDouble() = double
        override fun nextInt(until: Int) = index
    }

    @Test
    fun `a region pays out only once per day`() {
        assertNull(rules.roll(gangneung, rewardedToday = true, visitedBefore = true, random = FixedRandom(0.0, 0)))
    }

    @Test
    fun `first visit always gives the region material and a 말랑`() {
        val reward = rules.roll(gangneung, rewardedToday = false, visitedBefore = false, random = FixedRandom(0.99, 2))!!
        assertEquals(MaterialIds.MOON_DEW, reward.material)
        assertEquals(1, reward.materialCount)
        assertEquals(SpeciesIds.TIDE_LOTUS, reward.seed)
    }

    @Test
    fun `repeat visits give a 말랑 only under the 30 percent chance`() {
        val lucky = rules.roll(gangneung, rewardedToday = false, visitedBefore = true, random = FixedRandom(0.29, 0))!!
        assertEquals(SpeciesIds.MOONBELL, lucky.seed)

        val unlucky = rules.roll(gangneung, rewardedToday = false, visitedBefore = true, random = FixedRandom(0.30, 0))!!
        assertNull(unlucky.seed)
        assertEquals(MaterialIds.MOON_DEW, unlucky.material)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown region is a programming error`() {
        rules.roll(RegionId("nowhere"), rewardedToday = false, visitedBefore = false, random = Random(1))
    }
}
