package org.blitzortung.android.app.preference;

import android.content.SharedPreferences;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class SharedPreferencesWrapper implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final SharedPreferences sharedPreferences;
    
    private final Set<OnSharedPreferenceChangeListener> listeners;

    @Inject
    public SharedPreferencesWrapper(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        listeners = new HashSet<OnSharedPreferenceChangeListener>();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        PreferenceKey preferenceKey = PreferenceKey.fromString(key);
        
        for (OnSharedPreferenceChangeListener listener : listeners) {
            listener.onSharedPreferenceChanged(sharedPreferences, preferenceKey);
        }
    }

    public SharedPreferences get() {
        return sharedPreferences;
    }

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener sharedPreferenceChangeListener) {
        listeners.add(sharedPreferenceChangeListener);
    }

    public interface OnSharedPreferenceChangeListener {
        /**
         * Called when a shared preference is changed, added, or removed. This
         * may be called even if a preference is set to its existing value.
         *
         * <p>This callback will be run on your main thread.
         *
         * @param sharedPreferences The {@link SharedPreferences} that received
         *            the change.
         * @param key The key of the preference that was changed, added, or
         *            removed.
         */
        void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key);
    }
}
