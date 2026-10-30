package app.plenty.vehicle

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GearPosition {
    /** Not connected yet, no car service or the property errored. Never a departure or an arrival. */
    UNKNOWN,
    PARK,

    /** Any selection other than park (D, R, N, manual gears). */
    IN_GEAR,
}

/**
 * Departure and arrival signal from the standard AAOS GEAR_SELECTION property (docs/API_FACTS.md):
 * the current value on subscribe, then one event per change (P=4, D=8 on the emulator). Needs
 * `android.car.permission.CAR_POWERTRAIN`. Kept apart from CarUxRestrictions on purpose: the
 * platform blocks the screen in D even at 0 km/h, which alone is not a departure.
 *
 * Uses registerCallback/status, deprecated in API 34 but the calls verified on the Pleos emulator;
 * switch to subscribePropertyEvents/propertyStatus only after verifying those there.
 */
@Suppress("DEPRECATION")
class GearSource(private val context: Context) {

    private val _position = MutableStateFlow(GearPosition.UNKNOWN)
    val position: StateFlow<GearPosition> = _position.asStateFlow()

    private var car: Car? = null
    private var properties: CarPropertyManager? = null
    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            _position.value = if (value.status == CarPropertyValue.STATUS_AVAILABLE) {
                (value.value as? Int).toPosition()
            } else {
                GearPosition.UNKNOWN
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            _position.value = GearPosition.UNKNOWN
        }
    }

    fun start() {
        if (car != null) return
        try {
            val connected = Car.createCar(context) ?: return
            val manager = connected.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            car = connected
            properties = manager
            val subscribed = manager.registerCallback(
                callback,
                VehiclePropertyIds.GEAR_SELECTION,
                CarPropertyManager.SENSOR_RATE_ONCHANGE,
            )
            if (!subscribed) Log.w(TAG, "GEAR_SELECTION subscription refused")
        } catch (e: Exception) {
            Log.w(TAG, "GEAR_SELECTION unavailable; journeys will not start or settle", e)
            _position.value = GearPosition.UNKNOWN
        }
    }

    fun stop() {
        properties?.unregisterCallback(callback)
        car?.disconnect()
        properties = null
        car = null
        _position.value = GearPosition.UNKNOWN
    }

    private fun Int?.toPosition() = when (this) {
        null, VehicleGear.GEAR_UNKNOWN -> GearPosition.UNKNOWN
        VehicleGear.GEAR_PARK -> GearPosition.PARK
        else -> GearPosition.IN_GEAR
    }

    private companion object {
        const val TAG = "GearSource"
    }
}
