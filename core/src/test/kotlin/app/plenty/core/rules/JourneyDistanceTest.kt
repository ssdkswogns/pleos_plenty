package app.plenty.core.rules

import app.plenty.core.content.DistanceConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class JourneyDistanceTest {

    private val config = DistanceConfig(maxJourneyKm = 2_000.0)

    @Test
    fun `difference of two readings is the journey distance`() {
        assertEquals(JourneyDistance.Valid(155.0), JourneyDistance.between(12_345.0, 12_500.0, config))
    }

    @Test
    fun `no movement is a valid zero distance`() {
        assertEquals(JourneyDistance.Valid(0.0), JourneyDistance.between(12_500.0, 12_500.0, config))
    }

    @Test
    fun `missing or unusable reading is unknown, never zero`() {
        assertEquals(JourneyDistance.Unknown, JourneyDistance.between(null, 12_500.0, config))
        assertEquals(JourneyDistance.Unknown, JourneyDistance.between(12_345.0, null, config))
        assertEquals(JourneyDistance.Unknown, JourneyDistance.between(12_345.0, Double.NaN, config))
        assertEquals(JourneyDistance.Unknown, JourneyDistance.between(-1.0, 10.0, config))
    }

    @Test
    fun `counter going backwards is invalid`() {
        assertEquals(
            JourneyDistance.Invalid(JourneyDistance.Reason.COUNTER_DECREASED),
            JourneyDistance.between(12_500.0, 30.0, config),
        )
    }

    @Test
    fun `implausible jump is invalid but the limit itself is valid`() {
        assertEquals(JourneyDistance.Valid(2_000.0), JourneyDistance.between(0.0, 2_000.0, config))
        assertEquals(
            JourneyDistance.Invalid(JourneyDistance.Reason.IMPLAUSIBLE_JUMP),
            JourneyDistance.between(0.0, 2_000.5, config),
        )
    }
}
