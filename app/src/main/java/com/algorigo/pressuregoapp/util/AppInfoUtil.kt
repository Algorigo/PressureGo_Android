package com.algorigo.pressuregoapp.util

import android.os.Build
import io.reactivex.rxjava3.core.Single

object AppInfoUtil {

    fun getSdkVersionSingle(): Single<Int> {
        return Single.create { emitter ->
            try {
                emitter.onSuccess(Build.VERSION.SDK_INT)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }
}