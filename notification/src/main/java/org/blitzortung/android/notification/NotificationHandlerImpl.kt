/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.notification


import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import org.blitzortung.android.common.alert.AlertResult
import org.blitzortung.android.common.alert.event.AlertEvent
import org.blitzortung.android.common.alert.event.AlertResultEvent
import org.blitzortung.android.common.background.BackgroundModeEvent
import org.blitzortung.android.common.preferences.PreferenceKey
import org.blitzortung.android.common.preferences.StringToFloatPreferenceProperty
import org.blitzortung.android.common.preferences.StringToLongPreferenceProperty
import org.blitzortung.android.common.protocol.ConsumerContainer
import org.blitzortung.android.common.protocol.ConsumerProperty
import org.blitzortung.android.notification.builder.LightningNotificationBuilder
import org.blitzortung.android.notification.signal.SignalManager
import org.jetbrains.anko.notificationManager

class NotificationHandlerImpl(alertContainer: ConsumerContainer<AlertEvent>,
                              sharedPreferences: SharedPreferences,
                              backgroundModeContainer: ConsumerContainer<BackgroundModeEvent>,
                              private val context: Context,
                              private val activityString: String,
                              private val lightningNotificationBuilder: LightningNotificationBuilder
): NotificationHandler {

    //We need this id to be able to remove/update the lightning notifications
    private val alarmNotificationID = 1

    private val notificationService: NotificationManager = context.notificationManager
    private val signalManager = SignalManager(context, sharedPreferences)

    private var notificationDistanceLimit: Float by StringToFloatPreferenceProperty(sharedPreferences, PreferenceKey.ALERT_NOTIFICATION_DISTANCE_LIMIT, "50")
    private var signalingDistanceLimit: Float by StringToFloatPreferenceProperty(sharedPreferences, PreferenceKey.ALERT_SIGNALING_DISTANCE_LIMIT, "25")

    private var signalingThresholdTime: Long by StringToLongPreferenceProperty(sharedPreferences, PreferenceKey.ALERT_SIGNALING_THRESHOLD_TIME, "25")
    private var latestSignalingTime: Long = 0

    private val backgroundMode: BackgroundModeEvent by ConsumerProperty(backgroundModeContainer) {
        oldValue, newValue ->

        //If the user opens the app, delete current LightningNotifications
        if(!oldValue.isInBackground && newValue.isInBackground)
            clearLightningNotifications()
    }

    private val alertConsumer = {event: AlertEvent ->

        //Only display notifications when the app is closed
        if (event is AlertResultEvent && backgroundMode.isInBackground) {
            val result = event.alertResult

            if(result != null) {
                if (result.closestStrikeDistance <= signalingDistanceLimit ) {
                    val currentTime = System.currentTimeMillis()

                    //Signaling thresholdtime is in minutes, so we need to multiply it with 1000 and 60
                    if(currentTime > latestSignalingTime + signalingThresholdTime * 1000 * 60) {
                        signal()

                        latestSignalingTime = currentTime
                    }
                }

                if(result.closestStrikeDistance < notificationDistanceLimit) {
                    sendNotification(activityString + ": " + getTextMessage(result, notificationDistanceLimit))
                } else {
                    clearLightningNotifications()
                }
            }
        }
    }

    private fun getTextMessage(alertResult: AlertResult, notificationDistanceLimit: Float): String {
        return alertResult.sectorsByDistance
                .filter { it.key <= notificationDistanceLimit }
                .map {
                    "%s %.0f%s".format(it.value.label, it.key, alertResult.parameters.measurementSystem.unitName)
                }.joinToString()
    }

    init {
        alertContainer.addConsumer(alertConsumer)
    }


    fun sendNotification(notificationText: String) {
        val notification = lightningNotificationBuilder.create(notificationText)

        notificationService.notify(alarmNotificationID, notification)
    }

    fun signal() {
        signalManager.signal()
    }

    override fun clearLightningNotifications() {
        notificationService.cancel(alarmNotificationID)
    }
}