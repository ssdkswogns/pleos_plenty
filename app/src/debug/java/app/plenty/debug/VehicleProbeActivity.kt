package app.plenty.debug

import android.car.Car
import android.car.VehicleGear as CarGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import app.plenty.ui.theme.PlentyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * T02 spike, debug builds only. Logs what the Vehicle SDK and CarUxRestrictions actually report,
 * so the real adapters (T08/T09) are built on observed behavior rather than assumptions.
 *
 * Launch: adb shell am start --user 10 -n app.plenty/.debug.VehicleProbeActivity
 * Read:   adb logcat -s PlentyProbe
 */
class VehicleProbeActivity : ComponentActivity() {

    private val lines = mutableStateListOf<String>()
    private val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private lateinit var vehicle: Vehicle
    private var car: Car? = null
    private var uxManager: CarUxRestrictionsManager? = null

    private val distanceListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(distance: Float?) = log("odometer update: $distance")
        override fun onFailed(e: Exception) = log("odometer listener failed: $e")
    }

    private val tripListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(distance: Float?) = log("trip(current) update: $distance")
        override fun onFailed(e: Exception) = log("trip listener failed: $e")
    }

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) = log("gear update: $gear")
        override fun onFailed(e: Exception) = log("gear listener failed: $e")
    }

    private val uxListener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { log("ux update: ${it.describe()}") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlentyTheme {
                LazyColumn(Modifier.fillMaxSize().padding(24.dp)) {
                    items(lines) { Text(it, style = PlentyTheme.typography.caption) }
                }
            }
        }
        probeUxRestrictions()
        probeVehicle()
    }

    override fun onDestroy() {
        propertyManager?.unregisterCallback(gearCallback)
        uxManager?.unregisterListener()
        car?.disconnect()
        if (::vehicle.isInitialized) {
            runCatching {
                vehicle.getOdometer().unregisterDistanceDriven(distanceListener)
                vehicle.getOdometer().unregisterTripCurrentDrivingDistance(tripListener)
                vehicle.getGear().unregisterGearSelection(gearListener)
            }.onFailure { log("unregister failed: $it") }
            vehicle.release()
        }
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        log("lifecycle: onStop (activity hidden)")
    }

    override fun onStart() {
        super.onStart()
        log("lifecycle: onStart")
    }

    private val gearCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) = log("GEAR_SELECTION event: ${value.value} status=${value.status}")
        override fun onErrorEvent(propertyId: Int, areaId: Int) = log("GEAR_SELECTION error area=$areaId")
    }
    private var propertyManager: CarPropertyManager? = null

    private fun probeUxRestrictions() {
        runCatching {
            val connected = Car.createCar(this) ?: error("Car.createCar returned null")
            car = connected
            val manager = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            uxManager = manager
            log("ux current: ${manager.currentCarUxRestrictions.describe()}")
            manager.registerListener(uxListener)
        }.onFailure { log("ux probe failed: $it") }
        runCatching {
            val properties = checkNotNull(car).getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            propertyManager = properties
            log("GEAR_SELECTION now: ${properties.getIntProperty(VehiclePropertyIds.GEAR_SELECTION, 0)} (P=${CarGear.GEAR_PARK}, D=${CarGear.GEAR_DRIVE})")
            log("GEAR_SELECTION subscribed: ${properties.registerCallback(gearCallback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)}")
        }.onFailure { log("gear property probe failed: $it") }
    }

    private fun probeVehicle() {
        runCatching {
            vehicle = Vehicle(applicationContext)
            vehicle.initialize()
            log("vehicle initialized")
        }.onFailure {
            log("vehicle initialize failed: $it")
            return
        }
        val odometer = vehicle.getOdometer()
        odometer.checkCurrentDistanceDrivenCapabilities(
            { log("odometer capability: $it") },
            { log("odometer capability failed: $it") },
        )
        odometer.getCurrentDistanceDriven({ log("odometer get: $it") }, { log("odometer get failed: $it") })
        odometer.getTripCurrentDrivingDistance({ log("trip(current) get: $it") }, { log("trip(current) get failed: $it") })
        odometer.getTripCumulativeDrivingDistance({ log("trip(cumulative) get: $it") }, { log("trip(cumulative) get failed: $it") })
        odometer.getTripSinceRefillDrivingDistance({ log("trip(sinceRefill) get: $it") }, { log("trip(sinceRefill) get failed: $it") })
        odometer.registerDistanceDriven(distanceListener)
        odometer.registerTripCurrentDrivingDistance(tripListener)

        val gear = vehicle.getGear()
        gear.getCurrentGearSelectionState({ log("gear get: $it") }, { log("gear get failed: $it") })
        gear.registerGearSelection(gearListener)
    }

    private fun CarUxRestrictions.describe(): String =
        "DO=$isRequiresDistractionOptimization active=0x${Integer.toHexString(activeRestrictions)}"

    private fun log(message: String) {
        Log.i(TAG, message)
        runOnUiThread { lines += "${clock.format(Date())}  $message" }
    }

    private companion object {
        const val TAG = "PlentyProbe"
    }
}
