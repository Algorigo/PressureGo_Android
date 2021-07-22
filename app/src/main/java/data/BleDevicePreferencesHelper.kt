package data

import android.content.Context

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

    companion object {
        const val KEY_DEVICE_STORAGE = "ble_devices.db"

        const val KEY_LATEST_SHOW_DEVICE_MAC_ADDRESS = "KEY_LATEST_SHOW_DEVICE_MAC_ADDRESS"

    }
}