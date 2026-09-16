package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Camera2CaptureHelper {
    private const val TAG = "Camera2CaptureHelper"

    fun captureIntruderPhoto(context: Context, onPhotoCaptured: (String?) -> Unit) {
        val intrudersDir = File(context.filesDir, "intruders").apply {
            if (!exists()) mkdirs()
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission not granted. Creating fallback log preview.")
            createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            Log.w(TAG, "CameraManager unavailable. Creating fallback image.")
            createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
            return
        }

        val backgroundThread = HandlerThread("IntruderCameraBackground").apply { start() }
        val backgroundHandler = Handler(backgroundThread.looper)

        // Safety timeout to prevent hanging camera access.
        var isCaptured = false
        val timeoutRunnable = Runnable {
            if (!isCaptured) {
                isCaptured = true
                Log.w(TAG, "Camera capture timed out. Using fallback image.")
                backgroundThread.quitSafely()
                createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
            }
        }
        backgroundHandler.postDelayed(timeoutRunnable, 4500)

        try {
            var frontCameraId: String? = null
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = cameraId
                    break
                }
            }

            // Fallback to any camera if front camera is not found.
            if (frontCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                frontCameraId = cameraManager.cameraIdList[0]
            }

            if (frontCameraId == null) {
                backgroundHandler.removeCallbacks(timeoutRunnable)
                backgroundThread.quitSafely()
                createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
                return
            }

            val characteristics = cameraManager.getCameraCharacteristics(frontCameraId)
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 270
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG)
            val size = if (!sizes.isNullOrEmpty()) {
                sizes.firstOrNull { it.width <= 1280 && it.height <= 960 } ?: sizes[0]
            } else {
                android.util.Size(640, 480)
            }

            val imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 2)

            imageReader.setOnImageAvailableListener({ reader ->
                if (isCaptured) {
                    reader.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }
                isCaptured = true
                backgroundHandler.removeCallbacks(timeoutRunnable)

                val image = reader.acquireLatestImage()
                if (image != null) {
                    val buffer: ByteBuffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()

                    try {
                        val file = File(intrudersDir, "intruder_${System.currentTimeMillis()}.jpg")
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        if (bitmap != null) {
                            val matrix = Matrix().apply {
                                postRotate(sensorOrientation.toFloat())
                                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                            }
                            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            FileOutputStream(file).use { outputStream ->
                                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                            }
                            rotatedBitmap.recycle()
                            bitmap.recycle()
                            onPhotoCaptured(file.absolutePath)
                        } else {
                            FileOutputStream(file).use { outputStream -> outputStream.write(bytes) }
                            onPhotoCaptured(file.absolutePath)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving intruder photo", e)
                        createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
                    }
                } else {
                    createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
                }

                try {
                    imageReader.close()
                    backgroundThread.quitSafely()
                } catch (_: Exception) {}
            }, backgroundHandler)

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    try {
                        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        captureBuilder.addTarget(imageReader.surface)
                        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

                        camera.createCaptureSession(
                            listOf(imageReader.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    try {
                                        val request = captureBuilder.build()
                                        session.capture(request, object : CameraCaptureSession.CaptureCallback() {
                                            override fun onCaptureCompleted(
                                                session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                result: TotalCaptureResult
                                            ) {
                                                super.onCaptureCompleted(session, request, result)
                                                try { camera.close() } catch (_: Exception) {}
                                            }
                                        }, backgroundHandler)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error capturing request", e)
                                        try { camera.close() } catch (_: Exception) {}
                                        if (!isCaptured) {
                                            isCaptured = true
                                            backgroundHandler.removeCallbacks(timeoutRunnable)
                                            backgroundThread.quitSafely()
                                            createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
                                        }
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    try { camera.close() } catch (_: Exception) {}
                                    if (!isCaptured) {
                                        isCaptured = true
                                        backgroundHandler.removeCallbacks(timeoutRunnable)
                                        backgroundThread.quitSafely()
                                        createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
                                    }
                                }
                            },
                            backgroundHandler
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating capture session", e)
                        try { camera.close() } catch (_: Exception) {}
                        if (!isCaptured) {
                            isCaptured = true
                            backgroundHandler.removeCallbacks(timeoutRunnable)
                            backgroundThread.quitSafely()
                            createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
                        }
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected, likely unavailable or claimed by another client")
                    try { camera.close() } catch (_: Exception) {}
                    if (!isCaptured) {
                        isCaptured = true
                        backgroundHandler.removeCallbacks(timeoutRunnable)
                        backgroundThread.quitSafely()
                        createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
                    }
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    val reason = when (error) {
                        CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "camera already in use by another app"
                        CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "maximum cameras already in use"
                        CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "camera disabled by system"
                        CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "camera device error"
                        CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "camera service error"
                        else -> "unknown camera error ($error)"
                    }
                    Log.w(TAG, "Intruder selfie skipped: $reason. Target app will not be interrupted.")
                    try { camera.close() } catch (_: Exception) {}
                    if (!isCaptured) {
                        isCaptured = true
                        backgroundHandler.removeCallbacks(timeoutRunnable)
                        backgroundThread.quitSafely()
                        createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
                    }
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera capture", e)
            backgroundHandler.removeCallbacks(timeoutRunnable)
            backgroundThread.quitSafely()
            createFallbackIntruderImage(context, intrudersDir, onPhotoCaptured)
        }
    }

    /**
     * Generates a preview image when the real camera cannot be used.
     * This is explicitly a fallback and is not presented as a real selfie.
     */
    private fun createFallbackIntruderImage(context: Context, dir: File, callback: (String?) -> Unit) {
        try {
            val width = 640
            val height = 480
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgPaint = Paint().apply {
                color = Color.rgb(20, 26, 38)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val circlePaint = Paint().apply {
                color = Color.rgb(220, 53, 69)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(width / 2f, height / 2f - 40, 100f, circlePaint)

            val headPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(width / 2f, height / 2f - 70, 36f, headPaint)
            canvas.drawArc(
                (width / 2f) - 60f, (height / 2f) - 40f,
                (width / 2f) + 60f, (height / 2f) + 60f,
                0f, -180f, true, headPaint
            )

            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText("⚠️ INTRUDER ALERT", width / 2f, height - 90f, textPaint)

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val subTextPaint = Paint().apply {
                color = Color.rgb(180, 190, 205)
                textSize = 20f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Camera unavailable • $dateStr", width / 2f, height - 50f, subTextPaint)

            val file = File(dir, "intruder_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { fos -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos) }
            bitmap.recycle()
            callback(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating fallback intruder image", e)
            callback(null)
        }
    }
}
