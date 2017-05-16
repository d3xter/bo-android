package org.blitzortung.android.common.preferences

import android.content.SharedPreferences

//Helper function to retrieve a preference value of a PreferenceKey
fun <T: Any> SharedPreferences.get(prefKey: org.blitzortung.android.common.preferences.PreferenceKey, default: T): T {
    val key = prefKey.toString()

    //Set<String> is not possible because of type erasure, so for Set<String> we still need to use the old way
    val value: Any = when(default) {
        is Long -> this.getLong(key, default)
        is Int -> this.getInt(key, default)
        is Boolean -> this.getBoolean(key, default)
        is String -> this.getString(key, default)
        is Float -> this.getFloat(key, default)
        else -> throw IllegalArgumentException("Type ${default::class} cannot be retrieved from a SharedPreference")
    }

    @Suppress("UNCHECKED_CAST")
    return value as T
}

fun <T: Any, V> SharedPreferences.getAndConvert(prefKey: org.blitzortung.android.common.preferences.PreferenceKey, default: T, convert: (T) -> V): V {
    val value = this.get(prefKey, default)
    return convert(value)
}

/**
 *  A generic extension function to set a SharedPreference-Value
 */
fun <T: Any> SharedPreferences.Editor.put(key: String, value: T) {
    when(value) {
        is String -> this.putString(key, value)
        is Int -> this.putInt(key, value)
        is Boolean -> this.putBoolean(key, value)
        is Float -> this.putFloat(key, value)
        is Long -> this.putLong(key, value)
        else -> throw IllegalArgumentException("Type ${value::class} cannoted be put inside a SharedPreference")
    }
}

fun <T: Any> SharedPreferences.Editor.put(key: org.blitzortung.android.common.preferences.PreferenceKey, value: T) {
    val keyString = key.key

    put(keyString, value)
}
