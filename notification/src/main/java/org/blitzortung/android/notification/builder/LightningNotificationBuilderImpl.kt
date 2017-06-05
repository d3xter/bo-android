package org.blitzortung.android.notification.builder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.blitzortung.android.common.util.isAtLeast

class LightningNotificationBuilderImpl(private val ctx: Context,
                                       private val cls: Class<*>,
                                       private val icon: Int,
                                       private val activityString: String,
                                       private val appName: String): LightningNotificationBuilder {

    override fun create(text: String): Notification {
        val intent = Intent(ctx, cls)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        return if (isAtLeast(Build.VERSION_CODES.JELLY_BEAN)) {
            createNotification(contentIntent, icon, text)
        } else {
            createLegacyNotification(contentIntent, icon, text)
        }
    }

    private fun createNotification(contentIntent: PendingIntent?, icon: Int, notificationText: String): Notification {
        return Notification.Builder(ctx)
                .setSmallIcon(icon)
                .setContentTitle(appName)
                .setContentText(notificationText)
                .setContentIntent(contentIntent)
                .setAutoCancel(true).build()
    }

    private fun createLegacyNotification(contentIntent: PendingIntent?, icon: Int, notificationText: String): Notification {
        val notification = Notification(icon, notificationText, System.currentTimeMillis())
        val setLatestEventInfo = Notification::class.java.getDeclaredMethod("setLatestEventInfo", Context::class.java, CharSequence::class.java, CharSequence::class.java, PendingIntent::class.java)
        setLatestEventInfo.invoke(notification, ctx, appName, notificationText, contentIntent)
        return notification
    }
}