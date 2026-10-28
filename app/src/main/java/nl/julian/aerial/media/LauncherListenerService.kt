package nl.julian.aerial.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import nl.julian.aerial.data.NotificationsRepository

/**
 * Lets the launcher read media sessions (Now Playing) and notifications.
 * Android TV has no settings screen for this; grant it once with:
 * adb shell cmd notification allow_listener nl.julian.aerial/nl.julian.aerial.media.LauncherListenerService
 */
class LauncherListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationsRepository.attach(this)
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationsRepository.detach(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification) = refresh()

    private fun refresh() {
        runCatching { activeNotifications }.getOrNull()?.let { NotificationsRepository.publish(this, it) }
    }
}
