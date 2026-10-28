package app.plenty.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.plenty.R

/** Noto Sans KR ships as one variable font; each weight pins the wght axis. */
val NotoSansKr = FontFamily(
    notoSansKr(FontWeight.Normal),
    notoSansKr(FontWeight.Medium),
    notoSansKr(FontWeight.SemiBold),
    notoSansKr(FontWeight.Bold),
)

@OptIn(ExperimentalTextApi::class)
private fun notoSansKr(weight: FontWeight) = Font(
    resId = R.font.noto_sans_kr,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Type scale from design-tokens.json. One design unit is treated as 1 dp/sp. */
@Immutable
data class PlentyTypography(
    val display: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    /** 22sp semibold label of primary actions (GroveButton). */
    val button: TextStyle,
    /** 16sp navigation label from the rail in 03-home; not for body copy. */
    val navLabel: TextStyle,
)

private fun style(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = NotoSansKr,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val DefaultPlentyTypography = PlentyTypography(
    display = style(48, 60, FontWeight.Bold),
    title = style(36, 48, FontWeight.Bold),
    heading = style(28, 38, FontWeight.Bold),
    body = style(24, 34, FontWeight.Normal),
    label = style(22, 30, FontWeight.Medium),
    caption = style(18, 26, FontWeight.Normal),
    button = style(22, 30, FontWeight.SemiBold),
    navLabel = style(16, 22, FontWeight.Medium),
)

val LocalPlentyTypography = staticCompositionLocalOf { DefaultPlentyTypography }
