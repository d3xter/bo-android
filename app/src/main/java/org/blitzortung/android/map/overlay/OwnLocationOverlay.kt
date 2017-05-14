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

package org.blitzortung.android.map.overlay

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import com.google.android.maps.ItemizedOverlay
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.app.R
import org.blitzortung.android.app.helper.ViewHelper
import org.blitzortung.android.common.preferences.PreferenceKey
import org.blitzortung.android.common.preferences.get
import org.blitzortung.android.location.LocationEvent
import org.blitzortung.android.map.OwnMapView
import org.blitzortung.android.map.components.LayerOverlayComponent
import org.blitzortung.android.util.TabletAwareView

class OwnLocationOverlay(context: Context, private val mapView: OwnMapView) : ItemizedOverlay<OwnLocationOverlayItem>(OwnLocationOverlay.DEFAULT_DRAWABLE), SharedPreferences.OnSharedPreferenceChangeListener, LayerOverlay {

    private val layerOverlayComponent: LayerOverlayComponent

    private var item: OwnLocationOverlayItem? = null

    private val sizeFactor: Float

    private var zoomLevel: Int = 0

    val locationEventConsumer: (LocationEvent) -> Unit = { event ->
        val location = event.location

        if (enabled) {
            item = location?.run { OwnLocationOverlayItem(location) }

            populate()
            refresh()
        }
    }

    init {
        layerOverlayComponent = LayerOverlayComponent(context.resources.getString(R.string.own_location_layer))

        item = null

        populate()

        mapView.addZoomListener { newZoomLevel ->
            if (newZoomLevel != zoomLevel) {
                zoomLevel = newZoomLevel
                refresh()
            }
        }

        zoomLevel = mapView.zoomLevel

        mapView.overlays.add(this)

        sizeFactor = ViewHelper.pxFromDp(mapView, 0.5f) * TabletAwareView.sizeFactor(context)

        val preferences = BOApplication.sharedPreferences
        preferences.registerOnSharedPreferenceChangeListener(this)
        onSharedPreferenceChanged(preferences, PreferenceKey.SHOW_LOCATION.toString())

        refresh()
    }

    override fun draw(canvas: Canvas?, mapView: com.google.android.maps.MapView?, shadow: Boolean) {
        if (!shadow) {
            super.draw(canvas, mapView, false)
        }
    }

    private fun refresh() {
        item?.run { setMarker(ShapeDrawable(OwnLocationShape(sizeFactor * zoomLevel))) }

        //Redraw when the OwnLocation is refreshed
        mapView.postInvalidate()
    }

    override fun createItem(i: Int): OwnLocationOverlayItem {
        return item!!
    }

    override fun size(): Int {
        return if (item == null) 0 else 1
    }

    fun enableOwnLocation() {
        enabled = true
        refresh()
    }

    fun disableOwnLocation() {
        enabled = false
        item = null

        refresh()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, keyString: String) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString))
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        if (key == PreferenceKey.SHOW_LOCATION) {
            val showLocation = sharedPreferences.get(key, false)

            if (showLocation) {
                enableOwnLocation()
            } else {
                disableOwnLocation()
            }
        }
    }

    override val name: String
        get() = layerOverlayComponent.name

    override var enabled: Boolean
        get() = layerOverlayComponent.enabled
        set(value) {
            layerOverlayComponent.enabled = value
        }

    override var visible: Boolean
        get() = layerOverlayComponent.visible
        set(value) {
            layerOverlayComponent.visible = value
        }

    companion object {

        private val DEFAULT_DRAWABLE: Drawable

        init {
            val shape = OwnLocationShape(1f)
            DEFAULT_DRAWABLE = ShapeDrawable(shape)
        }
    }
}
