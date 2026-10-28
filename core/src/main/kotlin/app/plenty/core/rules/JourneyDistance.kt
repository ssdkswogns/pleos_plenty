package app.plenty.core.rules

import app.plenty.core.content.DistanceConfig

/** Distance of one journey, derived from two total-odometer snapshots in km (GAME_RULES.md §5). */
sealed interface JourneyDistance {
    data class Valid(val km: Double) : JourneyDistance

    /** A reading is missing or unusable. Settle later; never treat this as 0 km. */
    data object Unknown : JourneyDistance

    /** No reward for this span; the caller takes the end reading as the new baseline. */
    data class Invalid(val reason: Reason) : JourneyDistance

    enum class Reason { COUNTER_DECREASED, IMPLAUSIBLE_JUMP }

    companion object {
        fun between(startKm: Double?, endKm: Double?, config: DistanceConfig): JourneyDistance {
            if (!startKm.isUsableReading() || !endKm.isUsableReading()) return Unknown
            val delta = endKm!! - startKm!!
            return when {
                delta < 0 -> Invalid(Reason.COUNTER_DECREASED)
                delta > config.maxJourneyKm -> Invalid(Reason.IMPLAUSIBLE_JUMP)
                else -> Valid(delta)
            }
        }

        private fun Double?.isUsableReading() = this != null && isFinite() && this >= 0
    }
}
