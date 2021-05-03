package com.algorigo.pressurego

class PressureGo {

    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("pressurego-lib")
        }
    }
}