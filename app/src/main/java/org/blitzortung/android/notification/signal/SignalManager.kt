package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.get
import org.blitzortung.android.util.PreferenceChangedListener

class SignalManager(private val context: Context, sharedPreferences: SharedPreferences): PreferenceChangedListener() {
    private val signals = mutableListOf<NotificationSignal>()

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.ALERT_VIBRATION_SIGNAL)
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.ALERT_SOUND_SIGNAL)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when(key) {
            PreferenceKey.ALERT_VIBRATION_SIGNAL -> {
                val vibrationDuration = sharedPreferences.get(key, 3L) * 10

                val vibrationSignal = signals.firstOrNull { it is VibrationSignal }

                if(vibrationSignal != null) {
                    (vibrationSignal as VibrationSignal).vibrationDuration = vibrationDuration
                } else {
                    signals.add(VibrationSignal(context, vibrationDuration))
                }
            }

            PreferenceKey.ALERT_SOUND_SIGNAL -> {
                val sound = sharedPreferences.get(key, "")

                if(sound.isNullOrEmpty()) {
                    signals.removeAll { it is SoundSignal }
                } else {
                    val currentSignal = signals.firstOrNull { it is SoundSignal }

                    if(currentSignal != null) {
                        (currentSignal as SoundSignal).soundUri = Uri.parse(sound)
                    } else {
                        signals.add(SoundSignal(context, Uri.parse(sound)))
                    }
                }
            }
        }
    }

    fun signal() {
        signals.forEach { it.signal() }
    }
}