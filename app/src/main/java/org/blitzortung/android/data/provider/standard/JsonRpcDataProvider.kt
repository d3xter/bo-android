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

package org.blitzortung.android.data.provider.standard

import android.content.SharedPreferences
import android.util.Log
import org.blitzortung.android.app.Main
import org.blitzortung.android.common.preferences.PreferenceKey
import org.blitzortung.android.common.preferences.get
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.beans.Strike
import org.blitzortung.android.data.provider.DataProvider
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.result.ResultEvent
import org.blitzortung.android.jsonrpc.JsonRpcClient
import org.blitzortung.android.util.TimeFormat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class JsonRpcDataProvider(
        preferences: SharedPreferences,
        private val agentSuffix: String
) : DataProvider(preferences, PreferenceKey.SERVICE_URL) {

    private lateinit var serviceUrl: String
    private val dataBuilder: DataBuilder
    private var nextId = 0

    init {
        dataBuilder = DataBuilder()

        Log.v(Main.LOG_TAG, "JsonRpcDataProvider($serviceUrl)")
    }

    override val type: DataProviderType = DataProviderType.RPC

    private fun setUpClient(): JsonRpcClient {
        val client = JsonRpcClient(serviceUrl, agentSuffix)

        return client.apply {
            connectionTimeout = 40000
            socketTimeout = 40000
        }
    }

    override fun reset() {
        nextId = 0
    }

    @Throws(JSONException::class)
    private fun addStrikes(response: JSONObject, result: ResultEvent): ResultEvent {
        val strikes = ArrayList<Strike>()
        val referenceTimestamp = getReferenceTimestamp(response)
        val strikes_array = response.get("s") as JSONArray
        for (i in 0..strikes_array.length() - 1) {
            strikes.add(dataBuilder.createDefaultStrike(referenceTimestamp, strikes_array.getJSONArray(i)))
        }
        if (response.has("next")) {
            nextId = response.get("next") as Int
        }
        return result.copy(strikes = strikes)
    }

    private fun addRasterData(response: JSONObject, result: ResultEvent, info: String): ResultEvent {
        val rasterParameters = dataBuilder.createRasterParameters(response, info)
        val referenceTimestamp = getReferenceTimestamp(response)

        val strikes_array = response.get("r") as JSONArray
        val strikes = ArrayList<Strike>()
        for (i in 0..strikes_array.length() - 1) {
            strikes.add(dataBuilder.createRasterElement(rasterParameters, referenceTimestamp, strikes_array.getJSONArray(i)))
        }

        return result.copy(strikes = strikes, rasterParameters = rasterParameters, incrementalData = false, referenceTime = referenceTimestamp)
    }

    @Throws(JSONException::class)
    private fun getReferenceTimestamp(response: JSONObject): Long {
        return TimeFormat.parseTime(response.getString("t"))
    }

    @Throws(JSONException::class)
    private fun addStrikesHistogram(response: JSONObject, result: ResultEvent): ResultEvent {
        var resultVar = result

        if (response.has("h")) {
            val histogram_array = response.get("h") as JSONArray

            val histogram = IntArray(histogram_array.length())

            for (i in 0..histogram_array.length() - 1) {
                histogram[i] = histogram_array.getInt(i)
            }
            resultVar = resultVar.copy(histogram = histogram)
        }

        return resultVar
    }

    override fun <T> retrieveData(retrieve: DataRetriever.() -> T): T {
        val client = setUpClient()

        val t = Retriever(client).retrieve()

        client.shutdown()

        return t
    }


    private inner class Retriever(val client: JsonRpcClient) : DataRetriever {
        override fun getStations(region: Int): List<Station> {
            val stations = ArrayList<Station>()

            try {
                val response = client.call("get_stations")
                val stations_array = response.get("stations") as JSONArray

                for (i in 0..stations_array.length() - 1) {
                    stations.add(dataBuilder.createStation(stations_array.getJSONArray(i)))
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            return stations
        }

        override fun getStrikesGrid(parameters: Parameters, result: ResultEvent): ResultEvent {
            var resultVar = result

            nextId = 0

            val intervalDuration = parameters.intervalDuration
            val intervalOffset = parameters.intervalOffset
            val rasterBaselength = parameters.rasterBaselength
            val countThreshold = parameters.countThreshold
            val region = parameters.region

            try {
                val response = client.call("get_strikes_grid", intervalDuration, rasterBaselength, intervalOffset, region, countThreshold)

                val info = "%.0f km".format(rasterBaselength / 1000f)
                resultVar = addRasterData(response, resultVar, info)
                resultVar = addStrikesHistogram(response, resultVar)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            Log.v(Main.LOG_TAG,
                    "JsonRpcDataProvider: read %d bytes (%d raster positions, region %d)".format(client.lastNumberOfTransferredBytes, resultVar.strikes?.size, region))
            return resultVar
        }

        override fun getStrikes(parameters: Parameters, result: ResultEvent): ResultEvent {
            var resultVar = result
            val intervalDuration = parameters.intervalDuration
            val intervalOffset = parameters.intervalOffset
            if (intervalOffset < 0) {
                nextId = 0
            }
            resultVar = resultVar.copy(incrementalData = (nextId != 0))

            try {
                val response = client.call("get_strikes", intervalDuration, if (intervalOffset < 0) intervalOffset else nextId)

                resultVar = addStrikes(response, resultVar)
                resultVar = addStrikesHistogram(response, resultVar)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            Log.v(Main.LOG_TAG,
                    "JsonRpcDataProvider: read %d bytes (%d new strikes)".format(client.lastNumberOfTransferredBytes, resultVar.strikes?.size))
            return resultVar
        }

    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: PreferenceKey) {
        when (key) {
            PreferenceKey.SERVICE_URL -> serviceUrl = toCheckedUrl(sharedPreferences.get(PreferenceKey.SERVICE_URL, ""))

            else -> {
            }
        }
    }

    private fun toCheckedUrl(serviceUrl: String): String {
        try {
            URL(serviceUrl)
        } catch (e: Exception) {
            Log.e(Main.LOG_TAG, "JsonRpcDataProvider.tocheckedUrl($serviceUrl) invalid")
            return DEFAULT_SERVICE_URL
        }
        return serviceUrl
    }

    companion object {
        private val DATE_TIME_FORMATTER = SimpleDateFormat("yyyyMMdd'T'HH:mm:ss")
        private val DEFAULT_SERVICE_URL = "http://bo-service.tryb.de/"

        init {
            val tz = TimeZone.getTimeZone("UTC")
            DATE_TIME_FORMATTER.timeZone = tz
        }
    }
}
