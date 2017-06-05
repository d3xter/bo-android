package org.blitzortung.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.app.Main
import org.blitzortung.android.common.util.LOG_TAG

class BackgroundDownloadReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(LOG_TAG, "BackgroundDownloadReceiver.onReceive()")

        //TODO !!!! current releasing of the wakelock will not work with async RxAndroid
        //First aquire the wakelock and then update the data in background
        BOApplication.wakeLock.acquire()
        BOApplication.dataHandler.updateDataInBackground()
    }
}