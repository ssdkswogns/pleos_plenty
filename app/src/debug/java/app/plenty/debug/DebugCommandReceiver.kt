package app.plenty.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.plenty.PlentyApplication
import app.plenty.core.content.RegionId
import app.plenty.core.content.SpeciesIds
import app.plenty.data.GardenRepository
import app.plenty.data.InventoryEntity
import app.plenty.data.ItemIds
import app.plenty.data.JourneySource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration

/**
 * Debug-only commands for demos and QA; release builds never contain this receiver.
 *
 *   adb shell am broadcast --user 10 -n app.plenty/.debug.DebugCommandReceiver -a app.plenty.debug.SEED_DEMO
 *   adb shell am broadcast --user 10 -n app.plenty/.debug.DebugCommandReceiver -a app.plenty.debug.RESET
 *
 * SEED_DEMO resets the garden, plants one 말랑 of each species and runs three MOCK journeys through
 * the real settlement path on the three previous days, so daily caps apply as in play and today's
 * cap is untouched. Journeys keep source = MOCK so they are never presented as vehicle data.
 */
class DebugCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as PlentyApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_RESET -> reset(app)
                    ACTION_SEED_DEMO -> {
                        reset(app)
                        seedDemo(app)
                    }
                    else -> Log.w(TAG, "unknown action ${intent.action}")
                }
                Log.i(TAG, "${intent.action} done")
            } catch (e: Exception) {
                Log.e(TAG, "${intent.action} failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun reset(app: PlentyApplication) {
        app.container.database.clearAllTables()
        app.container.gardenRepository.ensureStarterGift()
    }

    private suspend fun seedDemo(app: PlentyApplication) {
        val db = app.container.database
        val dao = db.gardenDao()
        val plants = listOf(SpeciesIds.MOONBELL, SpeciesIds.CLOUD_FERN, SpeciesIds.TIDE_LOTUS)
            .mapIndexed { index, species ->
                val item = ItemIds.mallang(species)
                dao.upsertInventory(InventoryEntity(item, (dao.quantity(item) ?: 0) + 1))
                app.container.gardenRepository.plantMallang(species, slot = index + 1)
            }
        val trips = listOf(
            plants to RegionId("busan"),
            plants.take(2) to RegionId("pyeongchang"),
            listOf(plants[1]) to RegionId("seoul"),
        )
        var odometerKm = 12_500.0
        trips.forEachIndexed { index, (companions, region) ->
            val daysAgo = (trips.size - index).toLong()
            val repo = GardenRepository(db, clock = Clock.offset(Clock.systemDefaultZone(), Duration.ofDays(-daysAgo)))
            val journey = repo.startJourney(companions, odometerKm, JourneySource.MOCK)
            odometerKm += 50.0
            Log.i(TAG, "mock journey $journey -> ${repo.settleJourney(journey, odometerKm, region)}")
        }
    }

    private companion object {
        const val TAG = "PlentyDebug"
        const val ACTION_SEED_DEMO = "app.plenty.debug.SEED_DEMO"
        const val ACTION_RESET = "app.plenty.debug.RESET"
    }
}
