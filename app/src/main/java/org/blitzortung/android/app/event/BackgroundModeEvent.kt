package org.blitzortung.android.app.event

import org.blitzortung.android.common.protocol.Event

data class BackgroundModeEvent(val isInBackground: Boolean): Event
