package nl.julian.aerial.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Settings backup as aerial-settings.json in the TV's Downloads folder,
 * so it survives a reset or can be copied to another TV.
 */
object Backup {
    const val FILE = "aerial-settings.json"

    /** Writes the backup; returns where it went, or null on failure. */
    suspend fun save(context: Context): String? = withContext(Dispatchers.IO) {
        val text = ConfigStore.get(context).export()
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val resolver = context.contentResolver
                // Replace our earlier backup rather than piling up copies.
                own(context)?.let { resolver.delete(it, null, null) }
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, FILE)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
                resolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
            } else {
                @Suppress("DEPRECATION")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE).writeText(text)
            }
            "Downloads/$FILE"
        }.getOrNull()
    }

    /** Restores the backup this app saved in Downloads; false if there's none or it can't be read. */
    suspend fun restoreSaved(context: Context): Boolean = withContext(Dispatchers.IO) {
        val text = runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                own(context)?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
            } else {
                @Suppress("DEPRECATION")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE).readText()
            }
        }.getOrNull() ?: return@withContext false
        ConfigStore.get(context).import(text)
    }

    /** Restores from a file the user picked. */
    suspend fun restore(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val text = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
            .getOrNull() ?: return@withContext false
        ConfigStore.get(context).import(text)
    }

    private fun own(context: Context): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        return context.contentResolver.query(
            collection, arrayOf(MediaStore.Downloads._ID), "${MediaStore.Downloads.DISPLAY_NAME} = ?", arrayOf(FILE), null,
        )?.use { c -> if (c.moveToFirst()) Uri.withAppendedPath(collection, c.getLong(0).toString()) else null }
    }
}
