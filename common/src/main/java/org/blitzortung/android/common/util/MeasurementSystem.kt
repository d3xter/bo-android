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

package org.blitzortung.android.common.util

enum class MeasurementSystem(val unitName: String, private val factor: Float) {
    METRIC("km", 1000.0f),
    IMPERIAL("mi.", 1609.344f);

    fun calculateDistance(meters: Float): Float {
        return meters / factor
    }
}
