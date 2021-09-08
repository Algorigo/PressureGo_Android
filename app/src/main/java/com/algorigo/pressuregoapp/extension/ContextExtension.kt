package com.algorigo.pressuregoapp.extension

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File


fun Context.shareCsvFile(file: File) {
    val shareIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this@shareCsvFile, "com.algorigo.pressuregoapp.provider", file))
        type = "text/csv"
    }
    startActivity(Intent.createChooser(shareIntent, "Share Csv"))
}