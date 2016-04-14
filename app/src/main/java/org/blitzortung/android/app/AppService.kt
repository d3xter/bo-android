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

package org.blitzortung.android.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import org.blitzortung.android.BOApplication
import org.blitzortung.android.alert.event.AlertEvent
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.data.DataChannel
import org.blitzortung.android.data.DataHandler
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.ClearDataEvent
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.data.provider.result.StatusEvent
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.util.Period
import java.util.*

class AppService protected constructor(private val handler: Handler, private val updatePeriod: Period) : Service(), Runnable, SharedPreferences.OnSharedPreferenceChangeListener {
    private val binder = DataServiceBinder()

    var period: Int = 0
        private set
    var backgroundPeriod: Int = 0
        private set

    private var lastParameters: Parameters? = null
    private var updateParticipants: Boolean = false
    var isEnabled: Boolean = false
        private set

    private val dataHandler: DataHandler = BOApplication.instance.dataHandler
    private val locationHandler: LocationHandler = BOApplication.instance.locationHandler
    private val alertHandler: AlertHandler = BOApplication.instance.alertHandler

    private var alertEnabled: Boolean = false
    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null
    private val wakeLock = BOApplication.instance.wakeLock

    private val dataEventConsumer = { event: DataEvent ->
        if (event is ClearDataEvent) {
            restart()
        } else if (event is ResultEvent) {
            lastParameters = event.parameters
            configureServiceMode()
        }

        releaseWakeLock()
    }

    @SuppressWarnings("UnusedDeclaration")
    constructor() : this(Handler(), Period()) {
        Log.d(Main.LOG_TAG, "AppService() created with new handler")
    }

    init {
        Log.d(Main.LOG_TAG, "AppService() create")
        AppService.instance = this
    }

    fun reloadData() {
        if (isEnabled) {
            restart()
        } else {
            dataHandler.updateData(setOf(DataChannel.STRIKES))
        }
    }

    fun dataHandler(): DataHandler {
        return dataHandler
    }

    override fun onCreate() {
        Log.i(Main.LOG_TAG, "AppService.onCreate()")
        super.onCreate()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

        dataHandler.requestUpdates(dataEventConsumer)

        onSharedPreferenceChanged(preferences, PreferenceKey.QUERY_PERIOD)
        onSharedPreferenceChanged(preferences, PreferenceKey.ALERT_ENABLED)
        onSharedPreferenceChanged(preferences, PreferenceKey.BACKGROUND_QUERY_PERIOD)
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_PARTICIPANTS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(Main.LOG_TAG, "AppService.onStartCommand() startId: $startId $intent")

        if (intent != null && RETRIEVE_DATA_ACTION == intent.action) {
            acquireWakeLock()

            Log.v(Main.LOG_TAG, "AppService.onStartCommand() acquired wake lock " + wakeLock)

            isEnabled = false
            handler.removeCallbacks(this)
            handler.post(this)
        }

        return Service.START_STICKY
    }

    private fun acquireWakeLock() {
        wakeLock.acquire()
    }

    fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            try {
                wakeLock.release()
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() " + wakeLock)
            } catch (e: RuntimeException) {
                Log.v(Main.LOG_TAG, "AppService.releaseWakeLock() failed", e)
            }

        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(Main.LOG_TAG, "AppService.onBind() " + intent)

        return binder
    }

    override fun run() {
        if (dataHandler.hasConsumers) {
            if (alertEnabled && backgroundPeriod > 0) {
                Log.v(Main.LOG_TAG, "AppService.run() in background")

                dataHandler.updateDatainBackground()
            } else {
                isEnabled = false
                handler.removeCallbacks(this)
            }
        } else {
            releaseWakeLock()

            val currentTime = Period.currentTime
            val updateTargets = HashSet<DataChannel>()

            if (updatePeriod.shouldUpdate(currentTime, period)) {
                updatePeriod.lastUpdateTime = currentTime
                updateTargets.add(DataChannel.STRIKES)

                if (updateParticipants && updatePeriod.isNthUpdate(10)) {
                    updateTargets.add(DataChannel.PARTICIPANTS)
                }
            }

            if (!updateTargets.isEmpty()) {
                dataHandler.updateData(updateTargets)
            }

            val statusString = "" + updatePeriod.getCurrentUpdatePeriod(currentTime, period) + "/" + period
            dataHandler.broadcastEvent(StatusEvent(statusString))
            // Schedule the next update
            handler.postDelayed(this, 1000)
        }
    }

    fun restart() {
        configureServiceMode()
        updatePeriod.restart()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(Main.LOG_TAG, "AppService.onDestroy()")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.ALERT_ENABLED -> {
                alertEnabled = sharedPreferences.get(key, false)

                configureServiceMode()
            }

            PreferenceKey.QUERY_PERIOD -> period = Integer.parseInt(sharedPreferences.get(key, "60"))

            PreferenceKey.BACKGROUND_QUERY_PERIOD -> {
                backgroundPeriod = Integer.parseInt(sharedPreferences.get(key, "0"))

                Log.v(Main.LOG_TAG, "AppService.onSharedPreferenceChanged() backgroundPeriod=%d".format(backgroundPeriod))
                discardAlarm()
                configureServiceMode()
            }

            PreferenceKey.SHOW_PARTICIPANTS -> updateParticipants = sharedPreferences.get(key, true)
        }
    }

    fun configureServiceMode() {
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() entered")
        val backgroundOperation = dataHandler.hasConsumers
        if (backgroundOperation) {
            if (alertEnabled && backgroundPeriod > 0) {
                locationHandler.enableBackgroundMode()
                locationHandler.updateProvider()
                createAlarm()
            } else {
                alertHandler.unsetAlertListener()
                discardAlarm()
            }
        } else {
            discardAlarm()
            if (dataHandler.isRealtime) {
                Log.v(Main.LOG_TAG, "AppService.configureServiceMode() realtime data")
                if (!isEnabled) {
                    isEnabled = true
                    handler.removeCallbacks(this)
                    handler.post(this)
                }
            } else {
                Log.v(Main.LOG_TAG, "AppService.configureServiceMode() historic data")
                isEnabled = false
                handler.removeCallbacks(this)
                if (lastParameters != null && lastParameters != dataHandler.activeParameters) {
                    dataHandler.updateData()
                }
            }
            locationHandler.disableBackgroundMode()
            Log.v(Main.LOG_TAG, "AppService.configureServiceMode() set alert event consumer")
        }
        Log.v(Main.LOG_TAG, "AppService.configureServiceMode() done")
    }

    private fun createAlarm() {
        if (alarmManager == null && dataHandler.hasConsumers && backgroundPeriod > 0) {
            Log.v(Main.LOG_TAG, "AppService.createAlarm() with backgroundPeriod=%d".format(backgroundPeriod))
            val intent = Intent(this, AppService::class.java)
            intent.action = RETRIEVE_DATA_ACTION
            pendingIntent = PendingIntent.getService(this, 0, intent, 0)

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager?
            if (alarmManager != null) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, (backgroundPeriod * 1000).toLong(), pendingIntent)
            } else {
                Log.e(Main.LOG_TAG, "AppService.createAlarm() failed")
            }
            this.alarmManager = alarmManager
        }
    }

    private fun discardAlarm() {
        val alarmManager = alarmManager
        if (alarmManager != null) {
            Log.v(Main.LOG_TAG, "AppService.discardAlarm()")
            alarmManager.cancel(pendingIntent)
            pendingIntent!!.cancel()

            pendingIntent = null
            this.alarmManager = null
        }
    }

    fun alertEvent(): AlertEvent {
        return alertHandler.alertEvent
    }

    inner class DataServiceBinder : Binder() {
        internal val service: AppService
            get() {
                Log.d(Main.LOG_TAG, "DataServiceBinder.getService() " + this@AppService)
                return this@AppService
            }
    }

    companion object {
        val RETRIEVE_DATA_ACTION = "retrieveData"

        var instance: AppService? = null
            private set
    }
}
