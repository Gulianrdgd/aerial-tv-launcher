package nl.julian.aerial.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import nl.julian.aerial.R

private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Plus Jakarta Sans (variable, bundled) — a wide geometric grotesk. */
val Jakarta = FontFamily(jakarta(300), jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))

object AerialType {
    /** Stacked clock digits. Tight tracking and line height, so the two lines read as one block. */
    val clock = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight(800),
        fontSize = 44.sp, lineHeight = 42.sp, letterSpacing = (-0.02).em,
    )
    /** Microscopic uppercase eyebrow ("WED, 15 JUL"). */
    val eyebrow = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight(600),
        fontSize = 10.sp, letterSpacing = 0.2.em,
    )
    val pillTitle = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(600), fontSize = 14.sp)
    val pillSub = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight(400), fontSize = 11.sp)
    val tileLabel = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight(600),
        fontSize = 13.sp, letterSpacing = 0.01.em,
    )
    val sectionLabel = TextStyle(
        fontFamily = Jakarta, fontWeight = FontWeight(600),
        fontSize = 11.sp, letterSpacing = 0.18.em,
    )
}
