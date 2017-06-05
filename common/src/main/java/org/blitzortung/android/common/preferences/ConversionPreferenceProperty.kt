package org.blitzortung.android.common.preferences

import android.content.SharedPreferences
import kotlin.reflect.KProperty

abstract class ConversionPreferenceProperty<T: Any, R: Any>(sharedPreferences: SharedPreferences,
                                                   prefKey: PreferenceKey,
                                                   default: R,
                                                   action: ((PreferenceKey, T) -> Unit)? = null
) {
    protected abstract fun fromConversion(value: R): T
    protected abstract fun toConversion(value: T): R

    private var preferenceProperty by PreferenceProperty<R>(sharedPreferences, prefKey, default) {
        key, value ->

        action?.invoke(key, fromConversion(value))
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return fromConversion(preferenceProperty)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        preferenceProperty = toConversion(value)
    }
}