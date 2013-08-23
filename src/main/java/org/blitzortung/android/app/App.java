package org.blitzortung.android.app;

import android.app.Application;
import com.google.inject.util.Modules;
import roboguice.RoboGuice;

public class App extends Application {
    @Override
    public void onCreate() {
        RoboGuice.setBaseApplicationInjector(this, RoboGuice.DEFAULT_STAGE, Modules.override(RoboGuice.newDefaultRoboModule(this)).with(new AppModule()));
    }
}
