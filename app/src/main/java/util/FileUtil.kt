package util

import android.content.Context
import android.os.Environment
import android.util.Log
import io.reactivex.rxjava3.core.Completable
import org.joda.time.DateTime
import service.CSVRecordService
import java.io.File
import java.io.FileOutputStream

object FileUtil {

    @JvmStatic
    fun getFile(context: Context, macAddress: String): File {
        val date = DateTime.now()
        val directory = File(
            context.getExternalFilesDir(null)?.absolutePath,
            "pressurego${File.separator}${date.toString("yyyyMMdd")}"
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        Log.d("seunghwan", File(directory, "${macAddress}-${date.toString("hhmmss")}.csv").absolutePath)
        return File(directory, "${macAddress}.csv")
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