package com.algorigo.pressuregoapp.dfu

import android.util.Log
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

object Utility {
    private val LOG_TAG = Utility::class.java.simpleName

    fun download(urlString: String, file: File, callback: ((Int) -> Unit)? = null) {
        var count: Int
        try {
            val url = URL(urlString)
            val connection: URLConnection = url.openConnection()
            connection.connect()

            // this will be useful so that you can show a tipical 0-100%
            // progress bar
            val lengthOfFile: Int = connection.contentLength

            // download the file
            val input: InputStream = BufferedInputStream(
                url.openStream(),
                8192
            )

            // Output stream
            val output = FileOutputStream(file)
            val data = ByteArray(1024)
            var total: Long = 0
            while ((input.read(data).also { count = it }) != -1) {
                total += count.toLong()
                // publishing the progress....
                // After this onProgressUpdate will be called
                callback?.invoke(((total * 100) / lengthOfFile).toInt())
                Log.e(LOG_TAG, ((total * 100) / lengthOfFile).toInt().toString())

                // writing data to file
                output.write(data, 0, count)
            }

            // flushing output
            output.flush()

            // closing streams
            output.close()
            input.close()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Get Firmware Error", e)
        }
    }

    fun downloadObservable(urlString: String, file: File): Observable<Int> {
        return Observable.create<Int> {
            var count: Int
            try {
                val url = URL(urlString)
                val connection: URLConnection = url.openConnection()
                connection.connect()

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                val lengthOfFile: Int = connection.contentLength

                // download the file
                val input: InputStream = BufferedInputStream(
                    url.openStream(),
                    8192
                )

                // Output stream
                val output = FileOutputStream(file)
                val data = ByteArray(1024)
                var total: Long = 0
                while ((input.read(data).also { count = it }) != -1) {
                    total += count.toLong()
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    it.onNext(((total * 100) / lengthOfFile).toInt())
                    Log.e(LOG_TAG, ((total * 100) / lengthOfFile).toInt().toString())

                    // writing data to file
                    output.write(data, 0, count)
                }

                // flushing output
                output.flush()

                // closing streams
                output.close()
                input.close()

                it.onComplete()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Get Firmware Error", e)
                it.onError(e)
            }
        }.subscribeOn(Schedulers.io())
    }
}