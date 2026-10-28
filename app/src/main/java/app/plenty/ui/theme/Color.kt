package app.plenty.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Raw palette from design-tokens.json (design kit v1.0). Prefer [PlentyColors] roles in UI code. */
object PlentyPalette {
    val Navy = Color(0xFF002C5F)
    val Blue = Color(0xFF377EAC)
    val Sky = Color(0xFF93BEDA)
    val Ice = Color(0xFFDCECF5)
    val White = Color(0xFFFFFFFF)
    val Pearl = Color(0xFFF5F8FB)
    val Gray = Color(0xFFD6E0E8)
    val Muted = Color(0xFF566E83)
    val Ink = Color(0xFF19364D)
    val Night = Color(0xFF0E2134)
    val NightSurface = Color(0xFF183349)
    val NightBorder = Color(0xFF365064)
    val Shadow = Color(0xFF173D5A)
}

/**
 * Semantic color roles. Blue (#377EAC) is only 4.43:1 on white, so [accent] is for large text,
 * icons and progress bars; small emphasized text uses [primary].
 */
@Immutable
data class PlentyColors(
    val background: Color,
    val surface: Color,
    val surfaceTonal: Color,
    val border: Color,
    val text: Color,
    val textSecondary: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val disabledSurface: Color,
    val disabledText: Color,
    val shadow: Color,
)

val LightPlentyColors = PlentyColors(
    background = PlentyPalette.Pearl,
    surface = PlentyPalette.White,
    surfaceTonal = PlentyPalette.Ice,
    border = PlentyPalette.Gray,
    text = PlentyPalette.Ink,
    textSecondary = PlentyPalette.Muted,
    primary = PlentyPalette.Navy,
    onPrimary = PlentyPalette.White,
    accent = PlentyPalette.Blue,
    disabledSurface = PlentyPalette.Gray,
    disabledText = PlentyPalette.Muted,
    shadow = PlentyPalette.Shadow,
)

val LocalPlentyColors = staticCompositionLocalOf { LightPlentyColors }
