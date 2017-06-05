package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import org.blitzortung.android.common.preferences.PreferenceKey
import org.blitzortung.android.common.preferences.PreferenceProperty

class SoundSignalContainer(
        private val context: Context,
        preferences: SharedPreferences,
        private val soundSignalProvider: (Context, Uri?) -> SoundSignal? = { context, uri ->
            defaultSoundSignalProvider(context, uri)
        }
) : NotificationSignal {

    private var soundSignal: SoundSignal? = null

    private val sound by PreferenceProperty<String>(preferences, PreferenceKey.ALERT_SOUND_SIGNAL, "") {
        _, value ->

        soundSignal = if (!value.isNullOrEmpty()) {
            soundSignalProvider.invoke(context, Uri.parse(value))
        } else {
            null
        }
    }


    override fun signal() {
        soundSignal?.signal()
    }
}

internal fun defaultSoundSignalProvider(
        context: Context,
        ringtoneUri: Uri?,
        ringtoneProvider: (context: Context, ringtoneUri: Uri) -> Ringtone? = ::defaultRingtoneProvider): SoundSignal? {
    return ringtoneUri?.let {
        return SoundSignal(context, { ringtoneProvider.invoke(context, ringtoneUri) })
    }
}

private fun defaultRingtoneProvider(context: Context, ringtoneUri: Uri): Ringtone? {
    return RingtoneManager.getRingtone(context, ringtoneUri)
}
