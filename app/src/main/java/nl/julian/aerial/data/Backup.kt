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
    const val THEME_FILE = "aerial-theme.json"

    /** Writes the backup; returns where it went, or null on failure. */
    suspend fun save(context: Context): String? = write(context, FILE, ConfigStore.get(context).export())

    /** Writes [text] as Downloads/[file]; returns where it went, or null on failure. */
    suspend fun write(context: Context, file: String, text: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val resolver = context.contentResolver
                // Replace our earlier backup rather than piling up copies.
                own(context, file)?.let { resolver.delete(it, null, null) }
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
                resolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
            } else {
                @Suppress("DEPRECATION")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file).writeText(text)
            }
            "Downloads/$file"
        }.getOrNull()
    }

    /** Restores the backup this app saved in Downloads; false if there's none or it can't be read. */
    suspend fun restoreSaved(context: Context): Boolean =
        read(context, FILE)?.let { ConfigStore.get(context).import(it) } ?: false

    /** Restores from a file the user picked. */
    suspend fun restore(context: Context, uri: Uri): Boolean =
        read(context, uri)?.let { ConfigStore.get(context).import(it) } ?: false

    /** Downloads/[file] as text, if this app saved it there and it can still be read. */
    suspend fun read(context: Context, file: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                own(context, file)?.let { uri -> context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
            } else {
                @Suppress("DEPRECATION")
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file).readText()
            }
        }.getOrNull()
    }

    suspend fun read(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull()
    }

    private fun own(context: Context, file: String): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        return context.contentResolver.query(
            collection, arrayOf(MediaStore.Downloads._ID), "${MediaStore.Downloads.DISPLAY_NAME} = ?", arrayOf(file), null,
        )?.use { c -> if (c.moveToFirst()) Uri.withAppendedPath(collection, c.getLong(0).toString()) else null }
    }
}
