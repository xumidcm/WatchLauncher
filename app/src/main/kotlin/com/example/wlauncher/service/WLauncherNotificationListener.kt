package com.example.wlauncher.service

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotifData(
    val key: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val time: Long,
    val icon: ImageBitmap?
)

class WLauncherNotificationListener : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<NotifData>>(emptyList())
        val notifications: StateFlow<List<NotifData>> = _notifications.asStateFlow()

        private var instance: WLauncherNotificationListener? = null

        fun isConnected() = instance != null

        fun dismissNotification(key: String) {
            instance?.cancelNotification(key)
        }
    }

    override fun onListenerConnected() {
        instance = this
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshNotifications()
    }

    private fun refreshNotifications() {
        try {
            val sbns = activeNotifications ?: return
            val pm = applicationContext.packageManager
            _notifications.value = sbns
                .filter { it.notification.flags and Notification.FLAG_ONGOING_EVENT == 0 }
                .sortedByDescending { it.postTime }
                .take(20)
                .map { sbn ->
                    val n = sbn.notification
                    val extras = n.extras
                    val appLabel = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
                    } catch (_: Exception) { sbn.packageName }

                    val iconBitmap = try {
                        val smallIcon = n.smallIcon
                        smallIcon?.loadDrawable(applicationContext)?.toBitmap(48, 48)?.asImageBitmap()
                    } catch (_: Exception) { null }

                    NotifData(
                        key = sbn.key,
                        appLabel = appLabel,
                        title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
                        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
                        time = sbn.postTime,
                        icon = iconBitmap
                    )
                }
        } catch (_: Exception) {}
    }
}
