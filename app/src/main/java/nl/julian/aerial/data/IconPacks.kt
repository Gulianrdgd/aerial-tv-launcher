package nl.julian.aerial.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Icon packs in the common ADW/Nova format: an app that declares one of the
 * theme intents and ships an appfilter.xml mapping apps to its drawables.
 */
object IconPacks {
    private val ACTIONS = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.gau.go.launcherex.theme")

    /** Installed packs as package to name. */
    fun installed(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        return ACTIONS.flatMap { action -> pm.queryIntentActivities(Intent(action), 0) }
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
    }

    /** The pack's icon for each app package it covers, as 96 px bitmaps. Off the main thread. */
    @SuppressLint("DiscouragedApi") // appfilter names drawables by string; there's no other way to look them up.
    suspend fun load(context: Context, pack: String): Map<String, Bitmap> = withContext(Dispatchers.IO) {
        runCatching {
            val res = context.packageManager.getResourcesForApplication(pack)
            val parser = res.getIdentifier("appfilter", "xml", pack).takeIf { it != 0 }?.let { res.getXml(it) }
                ?: XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(context.packageManager.getResourcesForApplication(pack).assets.open("appfilter.xml"), null)
                }
            val names = mutableMapOf<String, String>()
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") continue
                // component="ComponentInfo{com.example/com.example.MainActivity}"
                val component = parser.getAttributeValue(null, "component") ?: continue
                val pkg = component.substringAfter('{', "").substringBefore('/', "")
                val drawable = parser.getAttributeValue(null, "drawable") ?: continue
                if (pkg.isNotEmpty() && pkg !in names) names[pkg] = drawable
            }
            names.mapNotNull { (pkg, name) ->
                val id = res.getIdentifier(name, "drawable", pack).takeIf { it != 0 } ?: return@mapNotNull null
                runCatching { pkg to res.getDrawable(id, null).toBitmap(96, 96) }.getOrNull()
            }.toMap()
        }.getOrDefault(emptyMap())
    }
}
