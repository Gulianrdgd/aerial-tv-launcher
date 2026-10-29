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

/**
 * The Material 3 type scale in Google Sans Flex, with the small styles
 * raised to 14sp: on a 43" TV across the room, 11–12sp is too small to read.
 */
val AerialTypography: Typography = Typography().let { d ->
    fun TextStyle.gs() = copy(fontFamily = GoogleSansFlex)
    fun TextStyle.tv() = copy(fontFamily = GoogleSansFlex, fontSize = 14.sp, lineHeight = 20.sp)
    Typography(
        displayLarge = d.displayLarge.gs(), displayMedium = d.displayMedium.gs(), displaySmall = d.displaySmall.gs(),
        headlineLarge = d.headlineLarge.gs(), headlineMedium = d.headlineMedium.gs(), headlineSmall = d.headlineSmall.gs(),
        titleLarge = d.titleLarge.gs(), titleMedium = d.titleMedium.gs(), titleSmall = d.titleSmall.gs(),
        bodyLarge = d.bodyLarge.gs(), bodyMedium = d.bodyMedium.gs(), bodySmall = d.bodySmall.tv(),
        labelLarge = d.labelLarge.gs(), labelMedium = d.labelMedium.tv(), labelSmall = d.labelSmall.tv(),
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
 * Clock digits, Material 3 Expressive style as on the Pixel lock screen:
 * bold, with Google Sans Flex's roundness axis turned all the way up, and
 * tabular figures so the time doesn't shift as it changes.
 */
private val ClockFont = FontFamily(
    Font(
        R.font.google_sans_flex,
        weight = FontWeight(650),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(650),
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("opsz", 48f),
        ),
    )
)

val ClockStyle = TextStyle(
    fontFamily = ClockFont, fontWeight = FontWeight(650),
    fontSize = 32.sp, lineHeight = 36.sp,
    fontFeatureSettings = "tnum",
)

/** Small-caps label over each widget ("WEDNESDAY 28 OCT", "NOW SHOWING", …). */
val EyebrowStyle = TextStyle(
    fontFamily = GoogleSansFlex, fontWeight = FontWeight(600),
    fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.12.em,
)

/** [AerialTypography] with every weight moved by [delta] (Settings > Text weight), kept within 100..900. */
fun Typography.withWeight(delta: Int): Typography {
    if (delta == 0) return this
    fun TextStyle.shift() = copy(fontWeight = FontWeight(((fontWeight ?: FontWeight.Normal).weight + delta).coerceIn(100, 900)))
    return copy(
        displayLarge = displayLarge.shift(), displayMedium = displayMedium.shift(), displaySmall = displaySmall.shift(),
        headlineLarge = headlineLarge.shift(), headlineMedium = headlineMedium.shift(), headlineSmall = headlineSmall.shift(),
        titleLarge = titleLarge.shift(), titleMedium = titleMedium.shift(), titleSmall = titleSmall.shift(),
        bodyLarge = bodyLarge.shift(), bodyMedium = bodyMedium.shift(), bodySmall = bodySmall.shift(),
        labelLarge = labelLarge.shift(), labelMedium = labelMedium.shift(), labelSmall = labelSmall.shift(),
    )
}
