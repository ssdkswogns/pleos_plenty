package app.plenty.ui.motion

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.runtime.withFrameNanos
import app.plenty.core.GardenLayout
import app.plenty.ui.theme.PlentyMotion
import app.plenty.vehicle.DrivingRestriction
import app.plenty.vehicle.DrivingRestrictionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Ambient motion and touch reactions of the garden home (plenty-motion-kit). Read [time] and the
 * levels only inside draw lambdas so frames redraw without recomposing.
 */
@Stable
class HomeMotion internal constructor(private val scope: CoroutineScope) {

    internal val runningState = mutableStateOf(false)
    internal val clock = mutableFloatStateOf(0f)
    private val gust = Animatable(0f)
    private val wiggles = List(GardenLayout.SLOT_COUNT) { Animatable(0f) }

    /** False = draw the still composition (driving, unknown state, hidden, or reduced motion). */
    val running: Boolean get() = runningState.value

    /** Seconds of ambient motion time. */
    val time: Float get() = clock.floatValue

    /** 0..1 strength of a tap-triggered gust. */
    val gustLevel: Float get() = gust.value

    /** 0..1 progress of the hop of the plant in [slot] (0-based). */
    fun wiggleLevel(slot: Int): Float = wiggles[slot].value

    fun gust() {
        if (!running) return
        scope.launch {
            gust.animateTo(1f, tween(400))
            gust.animateTo(0f, tween(2200))
        }
    }

    fun wiggle(slot: Int) {
        if (!running || slot !in wiggles.indices) return
        scope.launch {
            val hop = wiggles[slot]
            hop.snapTo(0f)
            hop.animateTo(1f, tween(PlentyMotion.REVEAL_MS, easing = LinearEasing))
            hop.snapTo(0f)
        }
    }

    internal suspend fun settle() {
        gust.snapTo(0f)
        wiggles.forEach { it.snapTo(0f) }
        clock.floatValue = 0f
    }
}

/** Runs the motion clock only while [active]; otherwise everything snaps to the still pose. */
@Composable
fun rememberHomeMotion(active: Boolean): HomeMotion {
    val scope = rememberCoroutineScope()
    val motion = remember { HomeMotion(scope) }
    LaunchedEffect(active) {
        motion.runningState.value = active
        if (!active) {
            motion.settle()
            return@LaunchedEffect
        }
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) motion.clock.floatValue += min((now - last) / 1_000_000_000f, 0.05f)
                last = now
            }
        }
    }
    return motion
}

/**
 * Motion gate from plenty-motion-kit: parked && uxAllowed && visible && !reducedMotion.
 * UNKNOWN restriction (not confirmed yet) keeps the scene still.
 */
@Composable
fun rememberHomeMotionAllowed(): Boolean {
    val restriction by rememberDrivingRestriction()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // Re-read on every lifecycle change so toggling "remove animations" applies on return.
    val animatorsEnabled = remember(lifecycleState) { ValueAnimator.areAnimatorsEnabled() }
    return restriction == DrivingRestriction.ALLOWED &&
        lifecycleState.isAtLeast(Lifecycle.State.STARTED) &&
        animatorsEnabled
}

@Composable
private fun rememberDrivingRestriction(): State<DrivingRestriction> {
    val context = LocalContext.current.applicationContext
    // android.car is optional; on devices without it the class cannot load and we stay restricted.
    val source = remember { runCatching { DrivingRestrictionSource(context) }.getOrNull() }
    DisposableEffect(source) {
        source?.start()
        onDispose { source?.stop() }
    }
    return (source?.state ?: remember { MutableStateFlow(DrivingRestriction.UNKNOWN) }).collectAsState()
}
