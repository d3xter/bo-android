package org.blitzortung.android.map.overlay;

public interface LayerOverlay {

    void setName(String name);
    
    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isVisible();

    void setVisibility(boolean visible);
}
