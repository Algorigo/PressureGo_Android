package data

import android.content.Context
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

class BleDevicePreferencesHelper(
    context: Context
) {

    private val preferences = context.getSharedPreferences(KEY_DEVICE_STORAGE, Context.MODE_PRIVATE)

    var latestShowDeviceMacAddress: String?
        get() = preferences.getString(KEY_LATEST_SHOW_DEVICE_MAC_ADDRESS, null)
        @Synchronized
        set(value) {
            preferences.edit().apply {
                putString(KEY_LATEST_SHOW_DEVICE_MAC_ADDRESS, value)
                apply()
            }
        }

    var latestSelectedMainButton: Boolean
        get() = preferences.getBoolean(KEY_LATEST_SELECTED_MAIN_BUTTON, true)
        @Synchronized
        set(value) {
            preferences.edit().apply {
                putBoolean(KEY_LATEST_SELECTED_MAIN_BUTTON, value)
                apply()
            }
        }

    var csvFileName: String?
        get() = preferences.getString(KEY_CSV_FILE_NAME, null)
        @Synchronized
        set(value) {
            preferences.edit().apply {
                putString(KEY_CSV_FILE_NAME, value)
                apply()
            }
        }

    fun setCsvFileNameSingle(value: String): Single<String> {
        return Single.create<String> { emitter ->
            try {
                csvFileName = value
                emitter.onSuccess(csvFileName)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }.subscribeOn(Schedulers.io())
    }

    companion object {
        const val KEY_DEVICE_STORAGE = "ble_devices.db"

        const val KEY_LATEST_SHOW_DEVICE_MAC_ADDRESS = "KEY_LATEST_SHOW_DEVICE_MAC_ADDRESS"
        const val KEY_CSV_FILE_NAME = "KEY_CSV_FILE_NAME"
        const val KEY_LATEST_SELECTED_MAIN_BUTTON = "KEY_LATEST_SELECTED_MAIN_BUTTON"

    }
}