package util

import android.content.Context
import android.util.Log
import io.reactivex.rxjava3.core.Completable
import org.joda.time.DateTime
import service.CSVRecordService
import java.io.File
import java.io.FileOutputStream

object FileUtil {

    @JvmStatic
    fun getFile(context: Context, macAddress: String, dateTime: DateTime): File {
        val directory = File(
            context.getExternalFilesDir(null)?.absolutePath,
            "pressurego${File.separator}${dateTime.toString("yyyyMMdd")}"
        )
        val file = File(directory, "${macAddress}-${dateTime.toString("hh-mm-ss")}.csv")
        if (!directory.exists()) {
            directory.mkdirs()
            val fileOutputStream = FileOutputStream(file, true)
            fileOutputStream.write("MacAdrress, DeviceName, DateTime, amp, sens, array[0], array[1], array[2], array[3]".toByteArray())
            fileOutputStream.close()
        }
        return file
    }

    @JvmStatic
    fun saveStringToFile(file: File, string: String): Completable {
        return Completable.create {
            val directory = file.parentFile
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    it.onError(IllegalStateException("mkdir failed"))
                    return@create
                }
            }
            val fileOutputStream = FileOutputStream(file, true)
            Log.d(CSVRecordService.TAG, "string == $string")
            fileOutputStream.write(string.toByteArray())
            fileOutputStream.close()
            it.onComplete()
        }
    }

}