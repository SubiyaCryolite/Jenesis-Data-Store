package io.github.subiyacryolite.jds.interfaces

import java.io.Serializable

interface IValue<T> : Serializable {

    var value: T

    fun get(): T {
        return value
    }

    fun set(value: T) {
        this.value = value
    }
}