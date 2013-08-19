package org.blitzortung.android.map.components;

import org.blitzortung.android.map.overlay.LayerOverlay;

public class LayerOverlayComponent implements LayerOverlay {

    public static final String DEFAULT_NAME = "<undefined>";
    private String name;
    private boolean visible;
    private boolean enabled;

    public LayerOverlayComponent() {
        name = DEFAULT_NAME;
        enabled = true;
        visible = true;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisibility(boolean visible) {
        this.visible = visible;
    }
}
