package app.plenty.vehicle

import app.plenty.core.content.RegionId

/**
 * Region of the arrival point. T10 replaces this with FusedLocation and administrative areas; until
 * then the region is unknown, which still pays distance XP but no region reward (DECISIONS.md §3).
 */
class ArrivalRegionSource {

    /** Set by debug builds (DebugCommandReceiver) to rehearse region rewards on the emulator. */
    @Volatile
    var override: RegionId? = null

    suspend fun current(): RegionId? = override
}
