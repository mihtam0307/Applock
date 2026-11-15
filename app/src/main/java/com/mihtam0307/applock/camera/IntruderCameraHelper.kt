package com.mihtam0307.applock.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chụp 1 tấm ảnh bằng camera trước ngay khi người dùng nhập sai mật khẩu
 * (hoặc khi cần ghi nhận người đang cố mở khoá), lưu vào bộ nhớ riêng của app
 * tại thư mục "intruder_photos". Ảnh chỉ được xem trong màn Lịch sử của chính app.
 */
object IntruderCameraHelper {

    fun capture(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onResult: (filePath: String?) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val imageCapture = ImageCapture.Builder().build()
                val selector = CameraSelector.DEFAULT_FRONT_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, imageCapture)

                val dir = File(context.filesDir, "intruder_photos").apply { mkdirs() }
                val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
                val file = File(dir, fileName)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            provider.unbindAll()
                            onResult(file.absolutePath)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            provider.unbindAll()
                            onResult(null)
                        }
                    }
                )
            } catch (e: Exception) {
                onResult(null)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
