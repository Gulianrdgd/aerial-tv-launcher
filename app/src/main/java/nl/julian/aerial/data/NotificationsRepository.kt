package nl.julian.aerial.data

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

@Immutable
data class TvNotification(
    val key: String,
    val pkg: String,
    val app: String,
    val title: String,
    val text: String?,
    val postTime: Long,
    val icon: ImageBitmap?,
    internal val contentIntent: PendingIntent?,
    internal val autoCancel: Boolean,
)

/**
 * Notifications other apps have posted, as reported by the launcher's
 * notification listener. Only dismissable ones are shown: ongoing ones
 * (downloads, media) are status, not messages.
 */
object NotificationsRepository {
    private val _items = MutableStateFlow<List<TvNotification>>(emptyList())
    val items: StateFlow<List<TvNotification>> = _items

    private var listener = WeakReference<NotificationListenerService>(null)
    private val iconCache = HashMap<String, ImageBitmap?>()

    internal fun attach(service: NotificationListenerService) {
        listener = WeakReference(service)
    }

    internal fun detach(service: NotificationListenerService) {
        if (listener.get() === service) listener = WeakReference(null)
        _items.value = emptyList()
    }

    internal fun publish(context: Context, active: Array<StatusBarNotification>) {
        _items.value = active.filter(::isMessage).sortedByDescending { it.postTime }.mapNotNull { sbn ->
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
            if (title.isNullOrBlank() && text.isNullOrBlank()) return@mapNotNull null
            TvNotification(
                key = sbn.key,
                pkg = sbn.packageName,
                app = appLabel(context, sbn.packageName),
                title = title ?: appLabel(context, sbn.packageName),
                text = text?.takeIf { it != title },
                postTime = sbn.postTime,
                icon = appIcon(context, sbn.packageName),
                contentIntent = sbn.notification.contentIntent,
                autoCancel = sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0,
            )
        }
    }

    internal fun isMessage(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        return sbn.isClearable && !sbn.isOngoing &&
            n.flags and Notification.FLAG_GROUP_SUMMARY == 0 &&
            n.category != Notification.CATEGORY_TRANSPORT
    }

    /** Opens the notification's target (as the app intended) and clears it if it auto-cancels. */
    fun open(n: TvNotification) {
        val intent = n.contentIntent ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                // The launcher is in front, so it may start the app on the sender's behalf.
                val options = ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                intent.send(null, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
        }
        if (n.autoCancel) dismiss(n)
    }

    fun dismiss(n: TvNotification) {
        runCatching { listener.get()?.cancelNotification(n.key) }
    }

    fun dismissAll() {
        runCatching { listener.get()?.cancelAllNotifications() }
    }

    private fun appLabel(context: Context, pkg: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun appIcon(context: Context, pkg: String): ImageBitmap? = iconCache.getOrPut(pkg) {
        runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }
}
