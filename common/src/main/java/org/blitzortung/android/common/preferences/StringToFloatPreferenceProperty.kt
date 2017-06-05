package org.blitzortung.android.common.preferences

import android.content.SharedPreferences

class StringToFloatPreferenceProperty(preferences: SharedPreferences, key: PreferenceKey, default: String): ConversionPreferenceProperty<Float, String>(preferences, key, default) {
    override fun fromConversion(value: String) = value.toFloat()

    override fun toConversion(value: Float) = value.toString()
}