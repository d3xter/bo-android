package org.blitzortung.android.notification.builder

import android.app.Notification

interface LightningNotificationBuilder {
    /**
     * Creates a new LightningNotification
     * @param[text] Text that will be displayed inside the notification
     */
    fun create(text: String): Notification
}