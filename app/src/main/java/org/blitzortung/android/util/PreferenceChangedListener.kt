package org.blitzortung.android.util

import android.content.SharedPreferences
import org.blitzortung.android.app.view.PreferenceKey

abstract class PreferenceChangedListener : SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(key))
    }

    abstract fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey)
}