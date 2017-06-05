package org.blitzortung.android.common.preferences

import android.content.SharedPreferences

class StringToLongPreferenceProperty(preferences: SharedPreferences, key: PreferenceKey, default: String): ConversionPreferenceProperty<Long, String>(preferences, key, default) {
    override fun fromConversion(value: String) = value.toLong()

    override fun toConversion(value: Long) = value.toString()
}