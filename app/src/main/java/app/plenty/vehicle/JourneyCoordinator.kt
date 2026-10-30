package app.plenty.vehicle

import android.util.Log
import app.plenty.core.content.RegionId
import app.plenty.data.GardenRepository
import app.plenty.data.JourneySource
import app.plenty.data.SettlementView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Turns gear changes into journeys (T08/T09, INTERACTIONS.md §05/06):
 *
 * - Out of park for [departureDebounceMs] after being parked: the ready selection departs with the
 *   odometer read at that moment (or the last reading taken while parked).
 * - In park for [arrivalDebounceMs]: the open journey settles with the end odometer and the arrival
 *   region, retried every [pendingRetryMs] while the odometer stays unreadable.
 *
 * A process that starts while already out of park never departs: a late reading would credit
 * distance nobody saw. An open journey found while parked settles right away, which covers app
 * restarts. The UX restriction signal plays no part here; settling twice is harmless because the
 * repository returns the stored result.
 */
@OptIn(FlowPreview::class)
class JourneyCoordinator(
    private val repository: GardenRepository,
    private val gear: Flow<GearPosition>,
    private val readOdometer: suspend () -> Double?,
    private val arrivalRegion: suspend () -> RegionId?,
    private val scope: CoroutineScope,
    private val departureDebounceMs: Long = 1_500,
    private val arrivalDebounceMs: Long = 3_000,
    private val pendingRetryMs: Long = 10_000,
) {
    private var job: Job? = null

    /** Last odometer seen while parked in this process: where the next departure starts. */
    private var parkedOdometerKm: Double? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            var previous = GearPosition.UNKNOWN
            gear.filter { it != GearPosition.UNKNOWN }
                .debounce { if (it == GearPosition.PARK) arrivalDebounceMs else departureDebounceMs }
                .distinctUntilChanged()
                .collectLatest { position ->
                    val departed = position == GearPosition.IN_GEAR && previous == GearPosition.PARK
                    previous = position
                    when (position) {
                        GearPosition.IN_GEAR -> if (departed) depart()
                        GearPosition.PARK -> settleWhileParked()
                        GearPosition.UNKNOWN -> Unit
                    }
                }
        }
    }

    private suspend fun depart() = withContext(NonCancellable) {
        val startKm = readOdometer() ?: parkedOdometerKm
        parkedOdometerKm = null
        if (startKm == null) {
            Log.w(TAG, "left park without an odometer reading; the ready selection waits for the next departure")
            return@withContext
        }
        runCatching { repository.depart(startKm, JourneySource.VEHICLE) }
            .onSuccess { Log.i(TAG, "departed: journey=$it start=$startKm km") }
            .onFailure { Log.e(TAG, "departure failed", it) }
    }

    private suspend fun settleWhileParked() {
        while (true) {
            val endKm = readOdometer()
            if (endKm != null) parkedOdometerKm = endKm
            val journeyId = repository.activeJourneyId() ?: return
            val view = withContext(NonCancellable) {
                runCatching { repository.settleJourney(journeyId, endKm, arrivalRegion()) }
                    .onFailure { Log.e(TAG, "settling journey $journeyId failed", it) }
                    .getOrNull()
            }
            Log.i(TAG, "arrived: journey=$journeyId end=$endKm km -> $view")
            if (view != null && view !is SettlementView.Pending) return
            delay(pendingRetryMs)
        }
    }

    private companion object {
        const val TAG = "JourneyCoordinator"
    }
}
