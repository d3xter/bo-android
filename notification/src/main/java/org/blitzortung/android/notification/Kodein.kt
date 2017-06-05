package org.blitzortung.android.notification

import android.content.Context
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import org.blitzortung.android.notification.builder.LightningNotificationBuilder
import org.blitzortung.android.notification.builder.LightningNotificationBuilderImpl

fun createNotificationKodeinModule(ctx: Context, mainActivity: Class<*>): Kodein.Module {

    return Kodein.Module {
        bind<NotificationHandler>() with singleton {
            NotificationHandlerImpl(instance(),
                sharedPreferences = instance(),
                context = ctx,
                activityString = instance("r.string.activity"),
                lightningNotificationBuilder = instance())
        }

        bind<LightningNotificationBuilder>() with singleton {
            LightningNotificationBuilderImpl(ctx,
                    mainActivity,
                    instance("r.drawable.icon"),
                    instance("r.string.activity"),
                    instance("r.string.app_name"))
        }
    }
}
