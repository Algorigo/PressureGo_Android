package com.algorigo.pressurego

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LockGetter<T> {
    private var value: T? = null
    private var continuation: Continuation<T>? = null

    fun setValue(value: T) {
        this.value = value
        continuation?.resume(value)
    }

    // This function suspends the calling coroutine and is resumed when continued with desired return object
    suspend fun getValue(): T {
        return if (value != null) {
            value!!
        } else {
            suspendCoroutine { continuation ->
                this.continuation = continuation
            }
        }
    }
}
