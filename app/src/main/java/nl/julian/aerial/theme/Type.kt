package nl.julian.aerial.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import nl.julian.aerial.R

private fun googleSans(weight: Int) = Font(
    R.font.google_sans_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Google Sans Flex (variable, bundled): Material 3 Expressive's typeface. */
val GoogleSansFlex = FontFamily(
    googleSans(200), googleSans(300), googleSans(400), googleSans(500), googleSans(600), googleSans(700),
)

/** The Material 3 type scale, set in Google Sans Flex. */
val AerialTypography: Typography = Typography().let { d ->
    fun TextStyle.gs() = copy(fontFamily = GoogleSansFlex)
    Typography(
        displayLarge = d.displayLarge.gs(), displayMedium = d.displayMedium.gs(), displaySmall = d.displaySmall.gs(),
        headlineLarge = d.headlineLarge.gs(), headlineMedium = d.headlineMedium.gs(), headlineSmall = d.headlineSmall.gs(),
        titleLarge = d.titleLarge.gs(), titleMedium = d.titleMedium.gs(), titleSmall = d.titleSmall.gs(),
        bodyLarge = d.bodyLarge.gs(), bodyMedium = d.bodyMedium.gs(), bodySmall = d.bodySmall.gs(),
        labelLarge = d.labelLarge.gs(), labelMedium = d.labelMedium.gs(), labelSmall = d.labelSmall.gs(),
    )
}

/** M3 shape scale, with Expressive's generous extra-large corner. */
val AerialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Clock digits: light (not hairline) so they read from the sofa, cut at a
 * large optical size, with tabular figures so the time doesn't shift as
 * it changes.
 */
private val ClockFont = FontFamily(
    Font(
        R.font.google_sans_flex,
        weight = FontWeight(340),
        variationSettings = FontVariation.Settings(FontVariation.weight(340), FontVariation.Setting("opsz", 72f)),
    )
)

val ClockStyle = TextStyle(
    fontFamily = ClockFont, fontWeight = FontWeight(340),
    fontSize = 64.sp, lineHeight = 66.sp, letterSpacing = (-0.01).em,
    fontFeatureSettings = "tnum",
)

/** Small-caps label over each widget ("WEDNESDAY 28 OCT", "NOW SHOWING", …). */
val EyebrowStyle = TextStyle(
    fontFamily = GoogleSansFlex, fontWeight = FontWeight(600),
    fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.14.em,
)
