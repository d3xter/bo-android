package org.blitzortung.android.location.provider

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import org.blitzortung.android.app.Main
import org.jetbrains.anko.locationManager

abstract class ManagerLocationProvider(protected val context: Context,
                                   var backgroundMode: Boolean = true,
                                   locationUpdate: (Location?) -> Unit,
                                   override val type: String)
: LocationProvider(locationUpdate), LocationListener {

    protected val locationManager = context.locationManager

    protected open val minTime: Long
        get() = if(backgroundMode) 12000 else 20000

    protected open val minDistance: Float
        get() = if(backgroundMode) 200f else 50f

    override fun start() {
        //Don't start the LocationProvider if we dont have any permissions
        if(!this.isPermissionGranted) {
            Log.d(Main.LOG_TAG, "Tried to start provider '$type' without permission granted")
            return
        }

        super.start()

        locationManager.requestLocationUpdates(type, minTime, minDistance, this)
    }

    override fun onLocationChanged(location: Location?) {
        //Don't send NULL locations to the listeners
        if(location is Location) {
            sendLocationUpdate(location)
        }
    }

    override fun onProviderDisabled(provider: String) { }

    override fun onProviderEnabled(provider: String) { }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }

    override val isEnabled: Boolean
        get() = locationManager.isProviderEnabled(type)


    override fun shutdown() {
        locationManager.removeUpdates(this)

        super.shutdown()
    }

    abstract val isPermissionGranted: Boolean
}
