package nl.julian.aerial.ui

import kotlinx.serialization.json.Json
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.tv.material3.LocalTextStyle
import java.util.Locale

/**
 * Translations live in assets/translations/<language>.json, one file per
 * language: the English text as it appears in the code, and its translation.
 * Texts with a number or a name in them use {0}, {1}… placeholders, e.g.
 * "{0} seconds": "{0} seconden"; the translation may move them around.
 * Anything without an entry (app names, show titles, new texts) stays English.
 * See CONTRIBUTING.md for adding a language.
 */
object Translations {
    @Volatile private var exact: Map<String, String> = emptyMap()
    @Volatile private var patterns: List<Pair<Regex, Pair<List<Int>, String>>> = emptyList()
    private val memo = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Loads the TV's language, if there's a file for it. Call once at startup. */
    fun load(context: Context) {
        val lang = Locale.getDefault().language
        val all = runCatching {
            val text = context.assets.open("translations/$lang.json").bufferedReader().use { it.readText() }
            Json.decodeFromString<Map<String, String>>(text)
        }.getOrDefault(emptyMap())
        val placeholder = Regex("""\{(\d)\}""")
        exact = all.filterKeys { !placeholder.containsMatchIn(it) }
        // "{0} first" -> ^(.+?) first$, remembering which placeholder each group is.
        // Longest fixed text first, so "Connected to {0} · …" wins over "{0} first".
        patterns = all.filterKeys { placeholder.containsMatchIn(it) }.map { (key, value) ->
            val order = placeholder.findAll(key).map { it.groupValues[1].toInt() }.toList()
            val regex = key.split(placeholder).joinToString("(.+?)") { Regex.escape(it) }
            Regex(regex) to (order to value)
        }.sortedByDescending { (r, _) -> r.pattern.length }
        memo.clear()
    }

    fun tr(text: String): String {
        if (exact.isEmpty() && patterns.isEmpty()) return text
        return memo.getOrPut(text) {
            exact[text] ?: patterns.firstNotNullOfOrNull { (regex, target) ->
                regex.matchEntire(text)?.let { m ->
                    val (order, value) = target
                    // Placeholders can hold a translatable text too, such as a setting's value.
                    order.foldIndexed(value) { i, out, n -> out.replace("{$n}", tr(m.groupValues[i + 1])) }
                }
            } ?: text
        }
    }
}

/** The text in the TV's language, when there's a translation for it. */
fun tr(text: String): String = Translations.tr(text)

/** tv-material's Text, translated with [tr]. Imported in place of androidx.tv.material3.Text. */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) = androidx.tv.material3.Text(
    tr(text), modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration,
    textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style,
)

/** The time in Dutch words, to the nearest five minutes: "tien over zeven", "half acht", "kwart voor acht". */
fun timeInDutch(hour24: Int, minute: Int): String {
    val m = (minute + 2) / 5 * 5
    val h = (hour24 + if (m >= 20) 1 else 0) % 12
    val name = listOf("twaalf", "één", "twee", "drie", "vier", "vijf", "zes", "zeven", "acht", "negen", "tien", "elf")[h]
    return when (m % 60) {
        0 -> "$name uur"
        5 -> "vijf over $name"
        10 -> "tien over $name"
        15 -> "kwart over $name"
        20 -> "tien voor half $name"
        25 -> "vijf voor half $name"
        30 -> "half $name"
        35 -> "vijf over half $name"
        40 -> "tien over half $name"
        45 -> "kwart voor $name"
        50 -> "tien voor $name"
        else -> "vijf voor $name"
    }
}
