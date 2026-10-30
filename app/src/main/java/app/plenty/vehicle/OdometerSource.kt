package app.plenty.vehicle

import ai.pleos.playground.vehicle.Vehicle
import android.content.Context
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Total distance driven in km from the Pleos Vehicle SDK (docs/API_FACTS.md, T02). Needs
 * `pleos.car.permission.CAR_DRIVE`. A read after a process restart includes the distance driven
 * meanwhile, so journeys survive the app being killed on the road.
 */
class OdometerSource(private val context: Context) {

    private var vehicle: Vehicle? = null

    fun start() {
        if (vehicle != null) return
        try {
            vehicle = Vehicle(context).also { it.initialize() }
        } catch (e: Exception) {
            Log.w(TAG, "Vehicle SDK unavailable; distances stay unknown", e)
        }
    }

    fun stop() {
        vehicle?.release()
        vehicle = null
    }

    /** The current reading, or null if the SDK fails or does not answer within [timeoutMs]. */
    suspend fun read(timeoutMs: Long = 3_000): Double? {
        val odometer = vehicle?.getOdometer() ?: return null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                odometer.getCurrentDistanceDriven(
                    { km ->
                        // Via the decimal text, so 1012.3f becomes 1012.3 and not 1012.2999877…
                        if (continuation.isActive) continuation.resume(km?.toString()?.toDouble())
                    },
                    { error ->
                        Log.w(TAG, "odometer read failed", error)
                        if (continuation.isActive) continuation.resume(null)
                    },
                )
            }
        }
    }

    private companion object {
        const val TAG = "OdometerSource"
    }
}
