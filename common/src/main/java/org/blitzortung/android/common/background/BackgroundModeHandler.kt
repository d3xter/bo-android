package org.blitzortung.android.common.background

import android.content.Context
import android.os.Handler
import android.util.Log
import org.blitzortung.android.common.protocol.ConsumerContainer
import org.blitzortung.android.common.util.LOG_TAG

class BackgroundModeHandler(private val context: Context)  {
    val handler = Handler(context.mainLooper)

    private var runnable: Runnable? = null

    val consumerContainer = object : ConsumerContainer<BackgroundModeEvent>() {
        override fun addedFirstConsumer() { }

        override fun removedLastConsumer() { }
    }

    fun updateBackgroundMode(isInBackground: Boolean) {

        //Post the isInBackground = true after 500ms
        //When the users switches activities, the flag is set to true and shortly after to false,
        //so we shouldn't send out a broadcast
        if(isInBackground) {
            runnable = Runnable {
                sendUpdates(isInBackground)

                runnable = null
            }

            handler.postDelayed(runnable, 500)
        } else {
            if(runnable is Runnable) {
                handler.removeCallbacks(runnable)

                runnable = null
            } else {
                sendUpdates(isInBackground)
            }
        }
    }

    fun sendUpdates(isInBackground: Boolean) {
        consumerContainer.storeAndBroadcast(BackgroundModeEvent(isInBackground))
        Log.d(LOG_TAG, "BackgroundModeHandler: Broadcasted isInBackground: $isInBackground")
    }

    fun requestUpdates(consumer: (BackgroundModeEvent) -> Unit) {
        consumerContainer.addConsumer(consumer)
    }

    fun removeUpdates(consumer: (BackgroundModeEvent) -> Unit) {
        consumerContainer.removeConsumer(consumer)
    }
}