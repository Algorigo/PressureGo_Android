package com.algorigo.pressuregoapp.util

import android.content.Context
import android.util.Log
import io.reactivex.rxjava3.core.Completable
import org.joda.time.DateTime
import com.algorigo.pressuregoapp.service.CSVRecordService
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object FileUtil {

    private val TAG: String = FileUtil::class.java.simpleName

    @JvmStatic
    fun getFile(context: Context, macAddress: String, dateTime: DateTime): File {
        val directory = File(
            context.getExternalFilesDir(null)?.absolutePath,
            "pressurego${File.separator}${dateTime.toString("yyyyMMdd")}"
        )
        val file = File(directory, "pressurego_${dateTime.toString("yyyy_MM_dd_HH_mm_ss")}.csv")
        if (!directory.exists()) {
            directory.mkdirs()
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
            if (getLastLineNumber(file) == 0) {
                fileOutputStream.write("MacAddress,TimeStamp,Amp,Sens,array[0],array[1],array[2],array[3]\n".toByteArray())
            }
            Log.d(CSVRecordService.TAG, "string == $string")
            fileOutputStream.write(string.toByteArray())
            fileOutputStream.close()
            it.onComplete()
        }
    }

    @JvmStatic
    fun getLastLineNumber(file: File): Int {
        val lines = mutableListOf<String>()
        val reader = Scanner(FileInputStream(file), "UTF-8")
        try {
            while (reader.hasNextLine()) {
                lines.add(reader.nextLine())
            }
            reader.close()
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "${e.printStackTrace()}")
        } catch (e: IOException) {
            Log.e(TAG, "${e.printStackTrace()}")
        }
        return lines.size
    }

    @Throws(IOException::class)
    fun getLinesNumber(file: File): Int {
        val br = BufferedReader(FileReader(file) as Reader)

        var lineCount = 0
        var line: String = ""
        while (br.readLine()?.also { line = it } != null) {
            if (line.length > 0) {
                lineCount++;
            }
        }
        return lineCount
    }

    @JvmStatic
    fun deleteFileCompletable(file: File?): Completable {
        return Completable.create {
            if(file == null || !file.exists()) {
                it.onError(IllegalStateException("file not existed"))
            }
            file?.delete()
            it.onComplete()
        }
    }
}