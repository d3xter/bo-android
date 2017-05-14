/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.common.preferences

enum class PreferenceKey(val key: String) {
    USERNAME("username"),
    PASSWORD("password"),
    SERVICE_URL("service_url"),
    RASTER_SIZE("raster_size"),
    COUNT_THRESHOLD("count_threshold"),
    MAP_TYPE("map_mode"),
    MAP_FADE("map_fade"),
    COLOR_SCHEME("color_scheme"),
    QUERY_PERIOD("query_period"),
    BACKGROUND_QUERY_PERIOD("background_query_period"),
    SHOW_PARTICIPANTS("show_participants"),
    SHOW_LOCATION("location"),
    ALERT_ENABLED("alarm_enabled"),
    ALERT_SOUND_SIGNAL("alarm_sound_signal"),
    ALERT_VIBRATION_SIGNAL("alarm_vibration_signal"),
    ALERT_NOTIFICATION_DISTANCE_LIMIT("notification_distance_limit"),
    ALERT_SIGNALING_DISTANCE_LIMIT("signaling_distance_limit"),
    ALERT_SIGNALING_THRESHOLD_TIME("signaling_threshold_time"),
    REGION("region"),
    DATA_SOURCE("data_source"),
    MEASUREMENT_UNIT("measurement_unit"),
    DO_NOT_SLEEP("do_not_sleep"),
    INTERVAL_DURATION("interval_duration"),
    HISTORIC_TIMESTEP("historic_timestep"),
    LOCATION_MODE("location_mode"),
    LOCATION_LONGITUDE("location_longitude"),
    LOCATION_LATITUDE("location_latitude");

    override fun toString(): String {
        return key
    }

    companion object {

        private val stringToValueMap = java.util.HashMap<String, PreferenceKey>()

        init {
            for (key in org.blitzortung.android.common.preferences.PreferenceKey.values()) {
                val keyString = key.toString()
                if (keyString in org.blitzortung.android.common.preferences.PreferenceKey.Companion.stringToValueMap) {
                    throw IllegalStateException("key value '%s' already defined".format(keyString))
                }
                org.blitzortung.android.common.preferences.PreferenceKey.Companion.stringToValueMap[keyString] = key
            }
        }

        fun fromString(string: String): org.blitzortung.android.common.preferences.PreferenceKey {
            return org.blitzortung.android.common.preferences.PreferenceKey.Companion.stringToValueMap[string]!!
        }
    }
}

//Helper function to retrieve a preference value of a PreferenceKey
fun <T: Any> android.content.SharedPreferences.get(prefKey: org.blitzortung.android.common.preferences.PreferenceKey, default: T): T {
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

fun <T: Any, V> android.content.SharedPreferences.getAndConvert(prefKey: org.blitzortung.android.common.preferences.PreferenceKey, default: T, convert: (T) -> V): V {
    val value = this.get(prefKey, default)
    return convert(value)
}

/**
 *  A generic extension function to set a SharedPreference-Value
 */
fun <T: Any> android.content.SharedPreferences.Editor.put(key: String, value: T) {
    when(value) {
        is String -> this.putString(key, value)
        is Int -> this.putInt(key, value)
        is Boolean -> this.putBoolean(key, value)
        is Float -> this.putFloat(key, value)
        is Long -> this.putLong(key, value)
        else -> throw IllegalArgumentException("Type ${value::class} cannoted be put inside a SharedPreference")
    }
}

fun <T: Any> android.content.SharedPreferences.Editor.put(key: org.blitzortung.android.common.preferences.PreferenceKey, value: T) {
    val keyString = key.key

    put(keyString, value)
}

interface OnSharedPreferenceChangeListener : android.content.SharedPreferences.OnSharedPreferenceChangeListener {
    fun onSharedPreferencesChanged(sharedPreferences: android.content.SharedPreferences, vararg keys: org.blitzortung.android.common.preferences.PreferenceKey) {
        keys.forEach { onSharedPreferenceChanged(sharedPreferences, it) }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, org.blitzortung.android.common.preferences.PreferenceKey.Companion.fromString(keyString))
    }

    fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences, key: org.blitzortung.android.common.preferences.PreferenceKey);
}