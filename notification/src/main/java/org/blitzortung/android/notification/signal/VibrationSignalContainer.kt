package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import org.blitzortung.android.common.preferences.OnSharedPreferenceChangeListener
import org.blitzortung.android.common.preferences.PreferenceKey
import org.blitzortung.android.common.preferences.PreferenceProperty
import org.blitzortung.android.common.preferences.get
import org.jetbrains.anko.vibrator

class VibrationSignalContainer(
        context: Context,
        preferences: SharedPreferences,
        val vibrationSignalProvider: (Long) -> VibrationSignal = { vibrationDuration ->
            defaultVibrationSignalProvider(context, vibrationDuration)
        }
) : NotificationSignal {

    lateinit private var vibrationSignal: VibrationSignal

    private val vibrationLength: Int by PreferenceProperty(preferences, PreferenceKey.ALERT_VIBRATION_SIGNAL, 3) {
        _, value ->

        vibrationSignal = vibrationSignalProvider(value * 10L)
    }

    override fun signal() {
        vibrationSignal.signal()
    }
}

internal fun defaultVibrationSignalProvider(
        context: Context,
        vibrationDuration: Long,
        vibrator: (Long) -> Unit = defaultVibrator(context)
): VibrationSignal {
    return VibrationSignal(vibrationDuration, vibrator)
}

private fun defaultVibrator(context: Context): (Long) -> Unit {
    return { vibrationDuration -> context.vibrator.vibrate(vibrationDuration) }
}