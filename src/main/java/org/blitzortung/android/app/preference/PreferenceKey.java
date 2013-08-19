package org.blitzortung.android.app.preference;

import java.util.HashMap;
import java.util.Map;

public enum PreferenceKey {
    USERNAME("username", String.class),
    PASSWORD("password", String.class),
    RASTER_SIZE("raster_size", Integer.class),
    MAP_TYPE("map_mode", String.class),
    MAP_FADE("map_fade", Integer.class),
    COLOR_SCHEME("color_scheme", String.class),
    QUERY_PERIOD("query_period", Integer.class),
    BACKGROUND_QUERY_PERIOD("background_query_period", Integer.class),
    SHOW_PARTICIPANTS("show_participants", Boolean.class),
    SHOW_LOCATION("location", Boolean.class),
    ALARM_ENABLED("alarm_enabled", Boolean.class),
    ALARM_SOUND_SIGNAL("alarm_sound_signal", String.class),
    ALARM_VIBRATION_SIGNAL("alarm_vibration_signal", Integer.class),
    NOTIFICATION_DISTANCE_LIMIT("notification_distance_limit", Integer.class),
    SIGNALING_DISTANCE_LIMIT("signaling_distance_limit", Integer.class),
    REGION("region", String.class),
    DATA_SOURCE("data_source", String.class),
    MEASUREMENT_UNIT("measurement_unit", String.class),
    DO_NOT_SLEEP("do_not_sleep", Boolean.class),
    INTERVAL_DURATION("interval_duration", Integer.class),
    HISTORIC_TIMESTEP("historic_timestep", Integer.class),
    LOCATION_MODE("location_mode", String.class),
    LOCATION_LONGITUDE("location_longitude", Double.class),
    LOCATION_LATITUDE("location_latitude", Double.class);
    
    private final String key;
    
    private final Class type;
    
    private PreferenceKey(String key, Class type) {
        this.key = key;
        this.type = type;
    }

    @Override
    public String toString()
    {
        return key;
    }

    private static Map<String, PreferenceKey> stringToValueMap = new HashMap<String, PreferenceKey>();
    static {
        for (PreferenceKey key : PreferenceKey.values()) {
            String keyString = key.toString();
            if (stringToValueMap.containsKey(keyString)) {
                throw new IllegalStateException(String.format("key value '%s' already defined", keyString));
            }
            stringToValueMap.put(keyString, key);
        }
    }

    public static PreferenceKey fromString(String string) {
        return stringToValueMap.get(string);
    }
}
