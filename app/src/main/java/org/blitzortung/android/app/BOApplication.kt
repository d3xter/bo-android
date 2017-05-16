package org.blitzortung.android.app

import android.app.Application
import android.content.pm.PackageInfo
import android.os.PowerManager
import android.preference.PreferenceManager
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.autoAndroidModule
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.common.alert.event.AlertEvent
import org.blitzortung.android.common.background.BackgroundModeHandler
import org.blitzortung.android.common.protocol.ConsumerContainer
import org.blitzortung.android.data.DataHandler
import org.blitzortung.android.location.LocationHandler
import org.blitzortung.android.notification.NotificationHandler
import org.blitzortung.android.notification.createNotificationKodeinModule
import org.jetbrains.anko.powerManager

class BOApplication : Application(), KodeinAware {
    override val kodein: Kodein by Kodein.lazy {
        //Register useful constants
        constant("r.string.app_name") with this@BOApplication.resources.getString(R.string.app_name)
        constant("r.string.activity") with this@BOApplication.resources.getString(R.string.activity)
        constant("r.drawable.icon") with R.drawable.icon


        bind<BackgroundModeHandler>() with singleton { BackgroundModeHandler(this@BOApplication) }
        bind<LocationHandler>() with singleton {
            LocationHandler(this@BOApplication,
                    backgroundModeHandler = instance(),
                    sharedPreferences = instance()) }

        bind<PowerManager.WakeLock>() with singleton { powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG) }
        bind<DataHandler>() with singleton {
            DataHandler(wakeLock = instance(),
                    agentSuffix = "-${getPackageInfo().versionCode}",
                    sharedPreferences = instance()) }

        bind<AlertHandler>() with singleton {
            AlertHandler(locationHandler = instance(),
                    dataHandler = instance(),
                    preferences = instance(),
                    context = this@BOApplication) }


        bind<ConsumerContainer<AlertEvent>>() with singleton { instance<AlertHandler>().alertConsumerContainer }

        import(createNotificationKodeinModule(this@BOApplication, Main::class.java))

        //Import default stuff from Kodein-Android
        import(autoAndroidModule(this@BOApplication))
    }

    override fun onCreate() {
        super.onCreate()

        //First of all, set the default values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)


        //Now instantiate the NotificationHandler, because it isn't used somewhere else
        notificationHandler = kodein.instance()
    }

    private fun getPackageInfo(): PackageInfo = packageManager.getPackageInfo(packageName, 0)

    companion object {
        lateinit var notificationHandler: NotificationHandler
            private set

        val WAKE_LOCK_TAG = "boAndroidWakeLock"
    }
}