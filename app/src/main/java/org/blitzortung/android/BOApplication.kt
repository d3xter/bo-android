package org.blitzortung.android

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.PowerManager
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.data.DataHandler
import org.blitzortung.android.location.LocationHandler
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.powerManager
import kotlin.properties.Delegates

class BOApplication: Application() {
    lateinit var locationHandler: LocationHandler
        private set

    lateinit var sharedPreferences: SharedPreferences
        private set

    lateinit var alertHandler: AlertHandler
        private set

    lateinit var dataHandler: DataHandler
        private set

    lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()

        BOApplication.instance = this

        sharedPreferences = applicationContext.defaultSharedPreferences

        val packageInfo = try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            throw IllegalStateException(e)
        }

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)

        dataHandler = DataHandler(wakeLock, packageInfo)

        locationHandler = LocationHandler(applicationContext, sharedPreferences)
        alertHandler = AlertHandler(locationHandler, sharedPreferences, this)
    }

    companion object {
        var instance: BOApplication by Delegates.notNull()
            private set

        val WAKE_LOCK_TAG = "boAndroidWakeLock"
    }
}