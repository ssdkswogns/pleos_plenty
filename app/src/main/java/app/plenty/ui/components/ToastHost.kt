package app.plenty.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.plenty.ui.theme.PlentyMotion
import app.plenty.ui.theme.PlentyTheme
import kotlinx.coroutines.delay

/** One short message at a time (FeedbackToast). Required information never lives only in a toast. */
@Stable
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    private var serial by mutableStateOf(0)

    fun show(text: String) {
        message = text
        serial++
    }

    internal val key: Int get() = serial

    internal fun dismiss() {
        message = null
    }
}

val LocalToast = staticCompositionLocalOf { ToastState() }

/** About 3 s, announced politely to screen readers, never covering a primary action area. */
@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val message = state.message
    LaunchedEffect(state.key) {
        if (message != null) {
            delay(TOAST_MS)
            state.dismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(PlentyMotion.PANEL_MS)),
        exit = fadeOut(tween(PlentyMotion.PRESS_MS)),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .background(PlentyTheme.colors.primary, CircleShape)
                .padding(horizontal = 32.dp, vertical = 18.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Text(
                text = message.orEmpty(),
                style = PlentyTheme.typography.label.copy(fontWeight = FontWeight.Medium),
                color = PlentyTheme.colors.onPrimary,
            )
        }
    }
}

private const val TOAST_MS = 3_000L
