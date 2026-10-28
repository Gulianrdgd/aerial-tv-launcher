package nl.julian.aerial.media

import android.service.notification.NotificationListenerService

/**
 * Exists only so the launcher may read active media sessions (Now Playing).
 * Android TV has no settings screen for this; grant it once with:
 * adb shell cmd notification allow_listener nl.julian.aerial/nl.julian.aerial.media.MediaListenerService
 */
class MediaListenerService : NotificationListenerService()
