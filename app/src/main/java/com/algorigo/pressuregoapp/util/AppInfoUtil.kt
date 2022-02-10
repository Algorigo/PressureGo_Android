package com.algorigo.pressuregoapp.util

import android.os.Build
import io.reactivex.rxjava3.core.Single

object AppInfoUtil {

    fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }
}
