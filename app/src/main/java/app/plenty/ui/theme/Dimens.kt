package app.plenty.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing scale from design-tokens.json. */
object PlentySpace {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
    val xxxl = 48.dp
}

object PlentyRadius {
    val button = 20.dp
    val slot = 20.dp
    val panel = 28.dp
    val rail = 32.dp
}

object PlentyTouch {
    val primaryHeight = 64.dp
    val minimumTarget = 56.dp
}

/** Motion durations (ms). Driving restriction stops all motion immediately. */
object PlentyMotion {
    const val PRESS_MS = 120
    const val PANEL_MS = 220
    const val REVEAL_MS = 600
    const val EVOLUTION_MS = 900
}
