package com.suda.androidscrcpy.utils

import android.content.res.AssetManager
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object AssetsUtil {

    fun copyAssetFileToInternalStorage(assetManager: AssetManager, assetPath: String, outputPath: String) {

        val inputStream: InputStream = assetManager.open(assetPath)
        val outputStream: OutputStream = FileOutputStream(outputPath)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }
}