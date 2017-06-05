package org.blitzortung.android.common.protocol

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * This Property consumes events of a ConsumerContainer
 * @property[consumerContainer] The ConsumerContainer this Property should register itself to
 * @property[action] This action will be called, when this Property receives a new value from the Container
 */
class ConsumerProperty<T: Any>(private val consumerContainer: ConsumerContainer<T>,
                               private val action: ((oldValue: T, newValue: T) -> Unit)? = null) {
    private var _value: T by Delegates.notNull<T>()

    init {
        consumerContainer.addConsumer { newValue ->
            action?.invoke(_value, newValue)

            _value = newValue
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return _value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        _value = value
    }
}
