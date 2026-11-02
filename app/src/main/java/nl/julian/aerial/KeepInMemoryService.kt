package nl.julian.aerial

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * "Keep Aerial in memory" (Settings › System). TVs with little memory kill
 * the launcher behind a streaming app, so Home has to start it from scratch.
 * A foreground service marks the app as in use, the way Projectivy's option
 * does. It does no work, and its notification is silent and out of sight.
 */
class KeepInMemoryService : Service() {
    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.app_name), NotificationManager.IMPORTANCE_MIN))
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ms_home)
            .setContentTitle(getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        runCatching { ServiceCompat.startForeground(this, ID, notification, type) }.onFailure { stopSelf() }
        return START_STICKY
    }

    companion object {
        private const val CHANNEL = "keep_in_memory"
        private const val ID = 1

        /** Call while Aerial is in front: Android only lets a visible app start one. */
        fun apply(context: Context, on: Boolean) {
            val intent = Intent(context, KeepInMemoryService::class.java)
            if (on) runCatching { ContextCompat.startForegroundService(context, intent) }
            else context.stopService(intent)
        }
    }
}
