package org.blitzortung.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.blitzortung.android.common.util.LOG_TAG
import org.blitzortung.android.data.DataHandler

class BackgroundDownloadReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(LOG_TAG, "BackgroundDownloadReceiver.onReceive()")

        //TODO !!!! current releasing of the wakelock will not work with async RxAndroid
        //First aquire the wakelock and then update the data in background
        context.appKodein().instance<PowerManager.WakeLock>().acquire()
        context.appKodein().instance<DataHandler>().updateDataInBackground()
    }
}