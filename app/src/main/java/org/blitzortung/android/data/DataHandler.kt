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

package org.blitzortung.android.data

import android.content.SharedPreferences
import android.os.PowerManager
import android.util.Log
import org.blitzortung.android.app.BOApplication
import org.blitzortung.android.app.Main
import org.blitzortung.android.common.preferences.OnSharedPreferenceChangeListener
import org.blitzortung.android.common.preferences.PreferenceKey
import org.blitzortung.android.common.preferences.get
import org.blitzortung.android.data.provider.DataProvider
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.data.provider.result.RequestStartedEvent
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.common.protocol.ConsumerContainer
import org.blitzortung.android.common.util.LOG_TAG
import org.jetbrains.anko.async
import org.jetbrains.anko.uiThread
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class DataHandler @JvmOverloads constructor(
        private val wakeLock: PowerManager.WakeLock,
        private val agentSuffix: String,
        private val dataProviderFactory: DataProviderFactory = DataProviderFactory()
) : OnSharedPreferenceChangeListener {

    private val sharedPreferences = BOApplication.sharedPreferences

    private val lock = ReentrantLock()
    private var dataProvider: DataProvider? = null
    var parameters = Parameters()
        private set

    lateinit private var parametersController: ParametersController

    private val dataConsumerContainer = object : ConsumerContainer<DataEvent>() {
        override fun addedFirstConsumer() {
            Log.d(LOG_TAG, "added first data consumer")
        }

        override fun removedLastConsumer() {
            Log.d(LOG_TAG, "removed last data consumer")
        }
    }

    private var dataMode = DataMode()

    init {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        onSharedPreferencesChanged(sharedPreferences, PreferenceKey.DATA_SOURCE, PreferenceKey.USERNAME, PreferenceKey.PASSWORD, PreferenceKey.RASTER_SIZE, PreferenceKey.COUNT_THRESHOLD, PreferenceKey.REGION, PreferenceKey.INTERVAL_DURATION, PreferenceKey.HISTORIC_TIMESTEP)

        updateProviderSpecifics()
    }

    fun requestUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.addConsumer(dataConsumer)
    }

    fun removeUpdates(dataConsumer: (DataEvent) -> Unit) {
        dataConsumerContainer.removeConsumer(dataConsumer)
    }

    val hasConsumers: Boolean
        get() = dataConsumerContainer.isEmpty

    fun updateData(updateTargets: Set<DataChannel> = DEFAULT_DATA_CHANNELS) {

        sendEvent(REQUEST_STARTED_EVENT)

        var updateParticipants = false
        if (updateTargets.contains(DataChannel.PARTICIPANTS)) {
            if (dataProvider!!.type == DataProviderType.HTTP || !dataMode.raster) {
                updateParticipants = true
            }
        }

        downloadAndBroadcastData(TaskParameters(parameters = parameters, updateParticipants = updateParticipants)) {
            result -> result?.let { sendEvent(it) }
        }
    }

    fun updateDataInBackground() {
        wakeLock.acquire()

        downloadAndBroadcastData(TaskParameters(parameters = parameters.copy(intervalDuration = 10), updateParticipants = false)) {
            result ->

            result?.let {
                sendEvent(it)
            }

            if (wakeLock.isHeld) {
                try {
                    wakeLock.release()
                    Log.v(LOG_TAG, "FetchBackgroundDataTask released wakelock " + wakeLock)
                } catch (e: RuntimeException) {
                    Log.e(LOG_TAG, "FetchBackgroundDataTask release wakelock failed ", e)
                }

            }
        }
    }

    val activeParameters: Parameters
        get() {
            if (dataMode.raster) {
                return parameters
            } else {
                var parameters = parameters
                if (!dataMode.region) {
                    parameters = parameters.copy(region = 0)
                }
                return parameters.copy(rasterBaselength = 0, countThreshold = 0)
            }
        }

    private fun downloadAndBroadcastData(taskParameters: TaskParameters, postDownload: (ResultEvent?) -> Unit) {
        BOApplication.async() {
            val parameters = taskParameters.parameters
            val flags = taskParameters.flags

            val result = if (lock.tryLock()) {
                try {
                    var result = ResultEvent(referenceTime = System.currentTimeMillis(), parameters = parameters, flags = flags)

                    dataProvider!!.retrieveData() {
                        if (parameters.isRaster()) {
                            result = getStrikesGrid(parameters, result)
                        } else {
                            result = getStrikes(parameters, result)
                        }

                        /*if (taskParameters.updateParticipants) {
                            result.copy(stations = getStations(parameters.region))
                        }*/
                    }

                    result
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                    ResultEvent(failed = true, parameters = parameters, flags = flags)
                } finally {
                    lock.unlock()
                }
            }
            else {
               null
            }

            uiThread {
                postDownload(result)
            }
        }
    }

    private fun sendEvent(dataEvent: DataEvent) {
        dataConsumerContainer.storeAndBroadcast(dataEvent)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE, PreferenceKey.SERVICE_URL -> {
                val providerTypeString = sharedPreferences.get(PreferenceKey.DATA_SOURCE, DataProviderType.RPC.toString())
                val providerType = DataProviderType.valueOf(providerTypeString.toUpperCase())
                val dataProvider = dataProviderFactory.getDataProviderForType(providerType, sharedPreferences, agentSuffix)
                this.dataProvider?.run { sharedPreferences.unregisterOnSharedPreferenceChangeListener(this@DataHandler.dataProvider) }
                this.dataProvider = dataProvider

                updateProviderSpecifics()
                updateData()
            }

            PreferenceKey.RASTER_SIZE -> {
                val rasterBaselength = Integer.parseInt(sharedPreferences.get(key, "10000"))
                parameters = parameters.copy(rasterBaselength = rasterBaselength);
                updateData()
            }

            PreferenceKey.COUNT_THRESHOLD -> {
                val countThreshold = Integer.parseInt(sharedPreferences.get(key, "1"))
                parameters = parameters.copy(countThreshold = countThreshold);
                updateData()
            }

            PreferenceKey.INTERVAL_DURATION -> {
                parameters = parameters.copy(intervalDuration = Integer.parseInt(sharedPreferences.get(key, "60")));
                updateData()
            }

            PreferenceKey.HISTORIC_TIMESTEP -> parametersController = ParametersController.withOffsetIncrement(
                    Integer.parseInt(sharedPreferences.get(key, "30")))

            PreferenceKey.REGION -> {
                val region = Integer.parseInt(sharedPreferences.get(key, "1"))
                parameters = parameters.copy(region = region);
                updateData()
            }

            else -> {
            }
        }
    }

    private fun updateProviderSpecifics() {

        val providerType = dataProvider!!.type

        dataMode = when (providerType) {
            DataProviderType.RPC -> DataMode(raster = true, region = false)

            DataProviderType.HTTP -> DataMode(raster = false, region = true)
        }
    }

    fun toggleExtendedMode() {
        dataMode = dataMode.copy(raster = dataMode.raster.xor(true))

        if (!isRealtime) {
            val dataChannels = HashSet<DataChannel>()
            dataChannels.add(DataChannel.STRIKES)
            updateData(dataChannels)
        }
    }

    val intervalDuration: Int
        get() = parameters.intervalDuration

    fun ffwdInterval(): Boolean {
        return updateParameters({ parametersController.ffwdInterval(it) })
    }

    fun rewInterval(): Boolean {
        return updateParameters({ parametersController.rewInterval(it) })
    }

    fun goRealtime(): Boolean {
        return updateParameters({ parametersController.goRealtime(it) })
    }

    fun updateParameters(updater: (Parameters) -> Parameters): Boolean {
        val oldParameters = parameters
        parameters = updater.invoke(parameters)
        return parameters != oldParameters
    }

    val isRealtime: Boolean
        get() = parameters.isRealtime()

    fun broadcastEvent(event: DataEvent) {
        dataConsumerContainer.broadcast(event)
    }

    companion object {
        val REQUEST_STARTED_EVENT = RequestStartedEvent()
        val DEFAULT_DATA_CHANNELS = setOf(DataChannel.STRIKES)
    }
}
