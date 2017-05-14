package org.blitzortung.android.common.preferences

import android.content.SharedPreferences
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * This class implements a Delegate to a SharedPreference
 *
 * @property sharedPreferences The SharedPreference to be used
 * @property prefKey They PreferenceKey this Property is working with
 * @property default Default value that will be used when there is no value set
 * @property action This Function will be called, when the property-value is updated through SharedPreference
 */
class PreferenceProperty<T: Any>(val sharedPreferences: SharedPreferences,
                                 val prefKey: PreferenceKey,
                                 private val default: T,
                                 private val action: ((PreferenceKey, T) -> Unit)? = null): OnSharedPreferenceChangeListener {
    private var _value: T by Delegates.notNull<T>()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        if(key == prefKey) {
             _value = sharedPreferences.get(key, default)

            action?.invoke(key, _value)
        }
    }

    init {
       sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(sharedPreferences, prefKey)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return _value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val editor = sharedPreferences.edit()

        with(editor) {
            put(prefKey, value)
            apply()
        }

        //TODO check, whether onSharedPreferenceChanged() is invoked
        _value = value
    }
}