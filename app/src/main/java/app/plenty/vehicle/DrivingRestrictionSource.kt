package app.plenty.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DrivingRestriction {
    /** Not connected yet or no car service. Treated exactly like [RESTRICTED]. */
    UNKNOWN,
    ALLOWED,
    RESTRICTED,
}

/**
 * Platform driving restriction from CarUxRestrictions (docs/API_FACTS.md, T02): gear D alone sets
 * DO=true, and the platform covers non-DO apps without stopping the activity, so game motion and
 * input must follow this signal rather than the activity lifecycle.
 */
class DrivingRestrictionSource(private val context: Context) {

    private val _state = MutableStateFlow(DrivingRestriction.UNKNOWN)
    val state: StateFlow<DrivingRestriction> = _state.asStateFlow()

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private val listener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener {
        _state.value = it.toRestriction()
    }

    fun start() {
        if (car != null) return
        try {
            val connected = Car.createCar(context) ?: return
            val uxManager = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            car = connected
            manager = uxManager
            _state.value = uxManager.currentCarUxRestrictions.toRestriction()
            uxManager.registerListener(listener)
        } catch (e: Exception) {
            Log.w(TAG, "CarUxRestrictions unavailable; staying restricted", e)
            _state.value = DrivingRestriction.UNKNOWN
        }
    }

    fun stop() {
        manager?.unregisterListener()
        car?.disconnect()
        manager = null
        car = null
        _state.value = DrivingRestriction.UNKNOWN
    }

    private fun CarUxRestrictions.toRestriction() =
        if (isRequiresDistractionOptimization) DrivingRestriction.RESTRICTED else DrivingRestriction.ALLOWED

    private companion object {
        const val TAG = "DrivingRestriction"
    }
}
