package app.plenty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object PlentyTheme {
    val colors: PlentyColors
        @Composable @ReadOnlyComposable get() = LocalPlentyColors.current

    val typography: PlentyTypography
        @Composable @ReadOnlyComposable get() = LocalPlentyTypography.current
}

/**
 * Plenty tokens are the source of truth. MaterialTheme is mapped onto them only so that
 * Material components (Text defaults, ripples, dialogs) pick up the same colors and font.
 */
@Composable
fun PlentyTheme(content: @Composable () -> Unit) {
    val colors = LightPlentyColors
    val typography = DefaultPlentyTypography
    CompositionLocalProvider(
        LocalPlentyColors provides colors,
        LocalPlentyTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                secondaryContainer = colors.surfaceTonal,
                background = colors.background,
                onBackground = colors.text,
                surface = colors.surface,
                onSurface = colors.text,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.border,
            ),
            typography = Typography(
                displayLarge = typography.display,
                headlineLarge = typography.title,
                titleLarge = typography.heading,
                bodyLarge = typography.body,
                labelLarge = typography.label,
                bodySmall = typography.caption,
            ),
            content = content,
        )
    }
}
