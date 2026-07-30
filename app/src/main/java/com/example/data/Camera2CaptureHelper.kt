package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

object Camera2CaptureHelper {
    private const val TAG = "Camera2CaptureHelper"

    fun captureIntruderPhoto(context: Context, onPhotoCaptured: (String?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission not granted")
            onPhotoCaptured(null)
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            onPhotoCaptured(null)
            return
        }

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

            // Fallback to any camera if front camera is not found
            if (frontCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                frontCameraId = cameraManager.cameraIdList[0]
            }

            if (frontCameraId == null) {
                onPhotoCaptured(null)
                return
            }

            val characteristics = cameraManager.getCameraCharacteristics(frontCameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG)
            val size = if (!sizes.isNullOrEmpty()) {
                sizes[0]
            } else {
                android.util.Size(640, 480)
            }

            val imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
            val handler = Handler(Looper.getMainLooper())

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val buffer: ByteBuffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()

                    try {
                        val file = File(context.filesDir, "intruder_${System.currentTimeMillis()}.jpg")
                        val outputStream = FileOutputStream(file)
                        outputStream.write(bytes)
                        outputStream.close()
                        onPhotoCaptured(file.absolutePath)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving intruder photo", e)
                        onPhotoCaptured(null)
                    }
                } else {
                    onPhotoCaptured(null)
                }
                imageReader.close()
            }, handler)

            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    try {
                        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        captureBuilder.addTarget(imageReader.surface)
                        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

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
                                                camera.close()
                                            }
                                        }, handler)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error capturing request", e)
                                        camera.close()
                                        onPhotoCaptured(null)
                                    }
                                }

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    camera.close()
                                    onPhotoCaptured(null)
                                }
                            },
                            handler
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating capture session", e)
                        camera.close()
                        onPhotoCaptured(null)
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    onPhotoCaptured(null)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    onPhotoCaptured(null)
                }
            }, handler)

        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
            onPhotoCaptured(null)
        }
    }
}
