package app.plenty

import android.app.Application
import android.util.Log
import app.plenty.data.GardenRepository
import app.plenty.data.PlentyDatabase
import app.plenty.vehicle.ArrivalRegionSource
import app.plenty.vehicle.GearPosition
import app.plenty.vehicle.GearSource
import app.plenty.vehicle.JourneyCoordinator
import app.plenty.vehicle.OdometerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf

class PlentyApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Process-wide singletons. Small enough that a DI framework would add more than it saves. */
class AppContainer(private val application: Application) {
    val database: PlentyDatabase = PlentyDatabase.create(application)
    val gardenRepository = GardenRepository(database)
    val arrivalRegion = ArrivalRegionSource()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var vehicleStarted = false

    /**
     * Connects the vehicle signals and starts turning gear changes into journeys. Lives as long as
     * the process, not the screen: the platform keeps the activity alive but covered while driving.
     */
    fun startVehicle() {
        if (vehicleStarted) return
        vehicleStarted = true
        // android.car is optional; without it the class cannot load and journeys never start.
        val gear = runCatching { GearSource(application) }
            .onFailure { Log.w(TAG, "android.car unavailable", it) }
            .getOrNull()
        val odometer = OdometerSource(application)
        gear?.start()
        odometer.start()
        JourneyCoordinator(
            repository = gardenRepository,
            gear = gear?.position ?: flowOf(GearPosition.UNKNOWN),
            readOdometer = { odometer.read() },
            arrivalRegion = arrivalRegion::current,
            scope = scope,
        ).start()
    }

    private companion object {
        const val TAG = "AppContainer"
    }
}
