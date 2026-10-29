package nl.julian.aerial.ui

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every translation file is valid JSON and keeps each text's {0}-style placeholders. */
class TranslationFilesTest {
    private val placeholder = Regex("""\{\d\}""")

    @Test
    fun translationFilesAreValid() {
        val files = File("src/main/assets/translations").listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue("no translation files found", files.isNotEmpty())
        for (file in files) {
            val texts = Json.decodeFromString<Map<String, String>>(file.readText())
            for ((english, translated) in texts) {
                assertEquals(
                    "${file.name}: \"$translated\" must keep the placeholders of \"$english\"",
                    placeholder.findAll(english).map { it.value }.toSet(),
                    placeholder.findAll(translated).map { it.value }.toSet(),
                )
            }
        }
    }
}
