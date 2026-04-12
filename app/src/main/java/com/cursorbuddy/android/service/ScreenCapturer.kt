package com.cursorbuddy.android.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.ByteArrayOutputStream

class ScreenCapturer(private val context: Context) {

    companion object {
        var mediaProjection: MediaProjection? = null
        var resultCode: Int = 0
        var resultData: Intent? = null
        
        fun hasPermission(): Boolean = resultData != null
    }

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    fun captureScreen(callback: (Bitmap?) -> Unit) {
        val data = resultData
        if (data == null || resultCode == 0) {
            callback(null)
            return
        }

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        try {
            mediaProjection?.stop()
            mediaProjection = projectionManager.getMediaProjection(resultCode, data.clone() as Intent)
        } catch (e: Exception) {
            callback(null)
            return
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        // Scale down for API efficiency (half resolution)
        val scaledWidth = width / 2
        val scaledHeight = height / 2

        imageReader = ImageReader.newInstance(scaledWidth, scaledHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "CursorBuddyCapture",
            scaledWidth, scaledHeight, density / 2,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null, handler
        )

        // Give it a moment to render
        handler.postDelayed({
            try {
                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * scaledWidth

                    val bitmap = Bitmap.createBitmap(
                        scaledWidth + rowPadding / pixelStride,
                        scaledHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    image.close()

                    // Crop to actual size (remove padding)
                    val cropped = Bitmap.createBitmap(bitmap, 0, 0, scaledWidth, scaledHeight)
                    if (cropped != bitmap) bitmap.recycle()

                    callback(cropped)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                callback(null)
            } finally {
                cleanup()
            }
        }, 150)
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 70): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun cleanup() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    fun release() {
        cleanup()
    }
}
