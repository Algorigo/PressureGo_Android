# Pressure Go

Pressure Go library for Android and Sample Application repository.

## Supporting Bluetooth Version

Tested with Bluetooth 5, Bluetooth 4.2 Devices.
This device is supporting Bluetooth Low Energy protocol.

### Bluetooth Specification

You can use Bluetooth characteristic like below.

#### Device Info Service
```
    UUID_DEVICE_INFO_SERVICE =       "0000180A-0000-1000-8000-00805F9B34FB"
    UUID_MANUFACTURER_NAME =         "00002A29-0000-1000-8000-00805F9B34FB"
    UUID_HARDWARE_REVISION =         "00002A27-0000-1000-8000-00805F9B34FB"
    UUID_FIRMWARE_REVISION =         "00002A26-0000-1000-8000-00805F9B34FB"
```

#### Device Generic Access Service
```
    UUID_GENERIC_ACCESS_SERVICE =    "00001800-0000-1000-8000-00805F9B34FB"
    UUID_DEVICE_NAME =               "00002A00-0000-1000-8000-00805F9B34FB"
    UUID_APPEARANCE =                "00002A01-0000-1000-8000-00805F9B34FB"
    UUID_PERIPHERAL_PREFERRED =      "00002A04-0000-1000-8000-00805F9B34FB"
    UUID_CENTRAL_ADDRESS_RESOL =     "00002AA6-0000-1000-8000-00805F9B34FB"
```

#### Battery Service
```
    UUID_BATTERY_SERVICE =           "0000180F-0000-1000-8000-00805F9B34FB"
    UUID_BATTERY_LEVEL =             "00002A19-0000-1000-8000-00805F9B34FB"
```

#### Communication Service
```
    UUID_SERVICE_DATA =              "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
    UUID_COMMUNICATION =             "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
    UUID_DATA_NOTIFICATION =         "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
```

#### Protocol
You can run commands like read and change value with writing characteristic to UUID_COMMUNICATION.

Data should be like below.

```kotlin
    //Change Value Command
    byteArrayOf(0x02, CODE, VALUE, 0x03)

    CODE_SENSOR_SCAN_INTERVAL = 0xa1.toByte()
    CODE_AMPLIFICATION =        0xc1.toByte()
    CODE_SENSITIVITY =          0xb1.toByte()

    //Read Value Command
    byteArrayOf(0x02, CODE, 0x03)

    CODE_SENSOR_SCAN_INTERVAL = 0xa2.toByte()
    CODE_AMPLIFICATION =        0xc2.toByte()
    CODE_SENSITIVITY =          0xb2.toByte()
```

Command Callback and Pressure Data are notified by characteristic UUID_DATA_NOTIFICATION.

Received Data will be like below.

```kotlin
    //Callback Data
    [0x02, CODE, VALUE, 0x03]

    //Pressure Data
    [0x01, BYTE_LOWER_1, BYTE_UPPER_1, BYTE_LOWER_2, BYTE_UPPER_2, BYTE_LOWER_3, BYTE_UPPER_3, BYTE_LOWER_4, BYTE_UPPER_4, 0x03]
    pressureValue[i] = (BYTE_LOWER[i] shl 8) + BYTE_UPPER[i]
```

## Sample Code

This repository shows how to use our device by android sample project.

### Integration

The easiest way to include the library to your project is to add the 

```implementation 'com.algorigo.device:pressurego:[Version]'``` 

line to your build.gradle file. 

### Usage

There is two way to use of pressure go library.

If you want to use [Android default bluetooth framework](https://developer.android.com/guide/topics/connectivity/bluetooth?hl=ko, "Android default bluetooth framework") and [kotlin coroutine](https://developer.android.com/kotlin/coroutines?hl=ko, "kotlin coroutine") you can use PDMSDevice class.

```kotlin
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import com.algorigo.pressurego.PDMSDevice
import kotlinx.coroutines.*

class PDMSActivity : AppCompatActivity() {

    private var pdmsDevice: PDMSDevice?
    private lateinit var intervalEditText: EditText
    private lateinit var amplificationEditText: EditText
    private lateinit var sensitivityEditText: EditText

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                pdmsDevice = PDMSDevice(device, this@BasicActivity)
            }
        }
    }

    private var dataCallback: PDMSDevice.DataCallback = object : PDMSDevice.DataCallback {
        override fun onData(intArray: IntArray) {
            runOnUiThread {
                setData(intArray)
            }
        }
    }

    private fun startScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter.bluetoothLeScanner.startScan(PDMSDevice.scanFilters, PDMSDevice.scanSettings, callback)
        }
    }

    private fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(callback)
    }

    private fun connect() {
        pdmsDevice?.connect(this)
    }

    private fun disconnect(macAddress: String) {
        pdmsDevice?.disconnect()
    }

    private fun getInterval() {
        CoroutineScope(Dispatchers.IO).async {
            val interval = try {
                pdmsDevice?.getSensingIntervalMillis()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                intervalEditText.setText(interval.toString())
            }
        }
    }

    private fun setInterval(interval: Int) {
        CoroutineScope(Dispatchers.IO).async {
            val interval = try {
                pdmsDevice?.setSensorScanIntervalMillis(interval)
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                intervalEditText.setText(interval.toString())
            }
        }
    }

    private fun getAmplification() {
        CoroutineScope(Dispatchers.IO).async {
            val amplification = try {
                pdmsDevice?.getAmplification()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                amplificationEditText.setText(amplification.toString())
            }
        }
    }

    private fun setAmplification(amplification: Int) {
        CoroutineScope(Dispatchers.IO).async {
            val amplification = try {
                pdmsDevice?.setAmplification(amplification)
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                amplificationEditText.setText(amplification.toString())
            }
        }
    }

    private fun getSensitivity() {
        CoroutineScope(Dispatchers.IO).async {
            val sensitivity = try {
                pdmsDevice?.getSensitivity()
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                sensitivityEditText.setText(sensitivity.toString())
            }
        }
    }

    private fun setSensitivity(sensitivity: Int) {
        CoroutineScope(Dispatchers.IO).async {
            val sensitivity = try {
                pdmsDevice?.setSensitivity(sensitivity)
            } catch (e: Exception) {
                null
            }
            runBlocking(Dispatchers.Main) {
                sensitivityEditText.setText(sensitivity.toString())
            }
        }
    }

    private fun startData() {
        pdmsDevice?.registerDataCallback(dataCallback)
    }

    private fun stopData() {
        pdmsDevice?.unregisterDataCallback(dataCallback)
    }

    private fun setData(intArray: IntArray) {
        //TODO 데이터     
    }
}
```

If you want to use [RxJava3](http://reactivex.io/, "RxJava3") and [AlgorigoBleLibrary](https://github.com/Algorigo/AlgorigoBleLibrary, "AlgorigoBleLibrary") which wraps [RxAndroidBle](https://github.com/Polidea/RxAndroidBle, "RxAndroidBle"), you can use RxPDMSDevice class.

You can import [AlgorigoBleLibrary](https://github.com/Algorigo/AlgorigoBleLibrary, "AlgorigoBleLibrary").

```implementation 'com.algorigo.rx:algorigoble:1.4.0'```

You can request permission easily with PermissionAppCompatActivity class of [AlgorigoUtils](https://github.com/Algorigo/AlgorigoUtils, "AlgorigoUtils").

```implementation 'com.algorigo.library:algorigoutil:1.3.0'```

```kotlin
import com.algorigo.algorigoble.BleManager
import com.algorigo.library.rx.permission.PermissionAppCompatActivity
import com.algorigo.pressurego.RxPDMSDevice
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class PDMSActivity : PermissionAppCompatActivity() {

    private var pdmsDevice: RxPDMSDevice?
    private var scanDisposable: Disposable? = null
    private var dataDisposable: Disposable? = null
    private lateinit var intervalEditText: EditText
    private lateinit var amplificationEditText: EditText
    private lateinit var sensitivityEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout)

        BleManager.init(applicationContext)
        BleManager.getInstance().bleDeviceDelegate = RxPDMSDevice.DeviceDelegate()
    }

    private fun startScan() {
        scanDisposable = requestPermissionCompletable(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    .andThen(BleManager.getInstance().scanObservable(5000).subscribeOn(Schedulers.io()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        devices = it.mapNotNull { it as? RxPDMSDevice }
                        adjustRecycler()
                    }, {
                        Log.e(LOG_TAG, "", it)
                    })
    }

    private fun stopScan() {
        scanDisposable?.dispose()
    }

    private fun connect() {
        pdmsDevice?.connectCompletable(false)
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                        Log.e(LOG_TAG, "connect complete")
                    }, {
                        Log.e(LOG_TAG, "connect error", it)
                    })
    }

    private fun disconnect(macAddress: String) {
        pdmsDevice?.disconnect()
    }

    private fun getInterval() {
        intervalEditText.setText(pdmsDevice?.getSensingIntervalMillis()?.toString())
    }

    private fun setInterval(interval: Int) {
        pdmsDevice?.setSensingIntervalMillisCompletable(interval)
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                    }, {
                        Log.e(LOG_TAG, "", it)
                    })
    }

    private fun getAmplification() {
        amplificationEditText.setText(pdmsDevice?.getAmplification()?.toString())
    }

    private fun setAmplification(amplification: Int) {
        pdmsDevice?.setAmplificationCompletable(amplification)
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                    }, {
                        Log.e(LOG_TAG, "", it)
                    })
    }

    private fun getSensitivity() {
        sensitivityEditText.setText(pdmsDevice?.getSensitivity()?.toString())
    }

    private fun setSensitivity(sensitivity: Int) {
        pdmsDevice?.setSensitivityCompletable(sensitivity)
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe({
                    }, {
                        Log.e(LOG_TAG, "", it)
                    })
    }

    private fun startData() {
        dataDisposable = pdmsDevice?.sendDataOn()
                        ?.observeOn(AndroidSchedulers.mainThread())
                        ?.subscribe({
                            setData(it)
                        }, {
                            Log.e(LOG_TAG, "", it)
                        })
    }

    private fun stopData() {
        dataDisposable?.dispose()
    }

    private fun setData(intArray: IntArray) {
        //TODO 데이터     
    }
}
```

You can update Firmware using our api wraps [Android-DFU-Library](https://github.com/NordicSemiconductor/Android-DFU-Library, "Android-DFU-Library").

You should create Android Service extending DfuBaseService of Android-DFU-Library.
See [Document of creating Service](https://github.com/NordicSemiconductor/Android-DFU-Library/tree/release/documentation#usage, "Document of creating Service").

When you use PDMSDevice class

```kotlin
    private var firmwarePath: Uri? = null // Local File path of firmware file

    private fun updateLocally() {
        firmwarePath?.also {
            CoroutineScope(Dispatchers.IO).async {
                try {
                    pdmsDevice?.update(
                        this@BasicPDMSDeviceActivity,
                        DfuService::class.java,
                        it
                    ) {
                        runOnUiThread {
                            Log.i(LOG_TAG, "update $it%")
                        }
                    }
                    runBlocking(Dispatchers.Main) {
                        Log.i(LOG_TAG, "update complete")
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Dfu Error", e)
                    runBlocking(Dispatchers.Main) {
                        Log.e(LOG_TAG, "", it)
                    }
                }
            }
        }
    }

    private fun updateRemotely() {
        CoroutineScope(Dispatchers.IO).async {
            val url = try {
                pdmsDevice?.checkUpdateExist()
            } catch (e: Exception) {
                null
            }
            if (url != null) {
                val file = File(ContextCompat.getDataDir(this), "temp.zip")
                try {
                    Utility.download(it, file) {
                        runOnUiThread {
                            Log.i(LOG_TAG, "download $it%")
                        }
                    }
                    pdmsDevice?.update(
                        this@BasicPDMSDeviceActivity,
                        DfuService::class.java,
                        file.absolutePath
                    ) {
                        runOnUiThread {
                            Log.i(LOG_TAG, "update $it%")
                        }
                    }
                    runBlocking(Dispatchers.Main) {
                        Log.i(LOG_TAG, "update complete")
                    }
                } catch (e: Exception) {
                    runBlocking(Dispatchers.Main) {
                        Log.e(LOG_TAG, "", it)
                    }
                } finally {
                    file.delete()
                }
            }
        }
    }
```

When you use RxPDMSDevice class

```kotlin
    private fun updateLocally() {
        disposable = firmwarePath?.let {
            pdmsDevice
                ?.update(this, DfuService::class.java, it)
                ?.doFinally {
                    disposable = null
                }
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    Log.i(LOG_TAG, "update $it%")
                }, {
                    Log.e(LOG_TAG, "", it)
                }, {
                    Log.i(LOG_TAG, "update complete")
                })
        }
    }

    private fun updateRemotely() {
        val file = File(ContextCompat.getDataDir(this), "temp.zip")
        pdmsDevice?.checkUpdateExist()
            ?.toSingle()
            ?.flatMapObservable {
                Utility.downloadObservable(it, file)
                    .ignoreElements()
                    .andThen(pdmsDevice?.update(this, DfuService::class.java, file.absolutePath))
            }
            ?.doFinally {
                file.delete()
            }
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
                Log.i(LOG_TAG, "update $it%")
            }, {
                Log.e(LOG_TAG, "", it)
            }, {
                Log.i(LOG_TAG, "update complete")
            })
    }
```

Remember to add your service to *AndroidManifest.xml*.

## Firmware Info
- [Firmware List](https://pressure-go.s3.ap-northeast-2.amazonaws.com/firmware/firmware.json "Firmware List")

## Store
- [Device Mart](https://www.devicemart.co.kr/main/index "Device Mart")

## Link
- [Algorigo](https://www.algorigo.com "Algorigo")

## Reporting Issues
For SDK feedback or to report a bug, please file a [GitHub Issue](https://github.com/Algorigo/PressureGo_Android/issues). For general suggestions or ideas, email us <[rouddy@algorigo.com](mailto:rouddy@algorigo.com)>.

## License
[MIT License](LICENSE)