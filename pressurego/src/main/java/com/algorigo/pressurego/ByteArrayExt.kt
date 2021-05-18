package com.algorigo.pressurego

fun ByteArray.toInt(): Int {
    var value = 0
    for (byte in reversed()) {
        value = (value shl 8) + ((byte.toInt() and 0xff))
    }
    return value
}