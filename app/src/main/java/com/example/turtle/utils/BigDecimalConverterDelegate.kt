package com.example.turtle.utils

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

class BigDecimalConverterDelegate(private val bigDecimalProperty: KMutableProperty<BigDecimal>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return bigDecimalProperty.getter.call().setScale(2, RoundingMode.HALF_UP).toString()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        bigDecimalProperty.setter.call(BigDecimal(value))
    }
}