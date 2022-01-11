package com.algorigo.pressuregoapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.algorigo.pressuregoapp.R
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class SplashActivity : AppCompatActivity() {

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onResume() {
        super.onResume()
        Completable.timer(1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .doFinally {
                disposable = null
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                moveToMain()
            }, {
                Log.e(LOG_TAG, "", it)
            })
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    private fun moveToMain() {
        startActivity(Intent(this, OnBoardingActivity::class.java))
        finish()
    }

    companion object {
        private val LOG_TAG = SplashActivity::class.java.simpleName
    }
}