package nl.julian.aerial.wallpaper

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * Your own photos and videos from removable storage (USB sticks, SD cards),
 * through the system's media library. The TV's internal storage is left out.
 */
object LocalMedia {
    val permissions: Array<String> =
        if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    fun hasAccess(context: Context) =
        permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    /** Call off the main thread. */
    fun load(context: Context): List<Aerial> {
        if (!hasAccess(context)) return emptyList()
        val volumes = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.getExternalVolumeNames(context) - MediaStore.VOLUME_EXTERNAL_PRIMARY
        } else setOf("external")
        return volumes.flatMap { volume ->
            query(context, MediaStore.Video.Media.getContentUri(volume), photo = false) +
                query(context, MediaStore.Images.Media.getContentUri(volume), photo = true)
        }
    }

    private fun query(context: Context, collection: Uri, photo: Boolean): List<Aerial> = runCatching {
        val columns = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
        context.contentResolver.query(collection, columns, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext() && size < MAX_PER_KIND) {
                    val uri = ContentUris.withAppendedId(collection, c.getLong(0))
                    val name = c.getString(1)?.substringBeforeLast('.') ?: ""
                    add(Aerial(name, uri.toString(), photo = photo, local = true))
                }
            }
        }
    }.getOrNull().orEmpty()

    /** A web link as a clip: a photo when it ends like an image, otherwise a video or live stream. */
    fun link(url: String): Aerial {
        val path = Uri.parse(url).path.orEmpty().lowercase()
        val photo = listOf(".jpg", ".jpeg", ".png", ".webp").any { path.endsWith(it) }
        return Aerial(Uri.parse(url).host ?: url, url, photo = photo, local = true)
    }

    private const val MAX_PER_KIND = 1000
}
