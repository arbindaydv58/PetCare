package com.example.petcare.utils

import android.content.Context
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File

object TestScreenshotHelper {

    private const val TAG = "TestScreenshotHelper"

    fun captureScreenshot(testFolder: String, screenshotName: String): String {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

        // Path: /sdcard/Android/data/com.example.petcare/files/test-results/<testFolder>/<screenshotName>.png
        val baseDir = context.getExternalFilesDir("test-results")
            ?: File(context.filesDir, "test-results")

        val targetFolder = File(baseDir, testFolder)
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }

        val fileName = if (screenshotName.endsWith(".png")) screenshotName else "$screenshotName.png"
        val destFile = File(targetFolder, fileName)

        val success = device.takeScreenshot(destFile)
        Log.i(TAG, "Screenshot $screenshotName captured at ${destFile.absolutePath}: success=$success")

        return destFile.absolutePath
    }
}
