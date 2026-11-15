package com.mihtam0307.applock.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chụp 1 tấm ảnh bằng camera trước mỗi khi có người cố mở 1 ứng dụng đã khoá
 * (dù nhập đúng hay sai mật khẩu), lưu vào bộ nhớ riêng của app tại thư mục
 * "intruder_photos". Ảnh được mã hoá bằng AES256-GCM (Android Keystore, qua
 * androidx.security.crypto.EncryptedFile) trước khi ghi xuống đĩa — kể cả khi
 * ai đó lấy được file .enc trực tiếp từ bộ nhớ máy cũng không mở xem được.
 * Ảnh chỉ được xem (sau khi tự giải mã) trong màn Lịch sử của chính app.
 */
object IntruderCameraHelper {

    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

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
                val fileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg.enc"
                val file = File(dir, fileName)
                if (file.exists()) file.delete() // EncryptedFile yêu cầu file đích chưa tồn tại

                val encryptedFile = EncryptedFile.Builder(
                    context, file, masterKey(context), EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                val outputStream = encryptedFile.openFileOutput()
                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            provider.unbindAll()
                            runCatching { outputStream.close() }
                            onResult(file.absolutePath)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            provider.unbindAll()
                            runCatching { outputStream.close() }
                            runCatching { file.delete() }
                            onResult(null)
                        }
                    }
                )
            } catch (e: Exception) {
                onResult(null)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Mở ảnh đã mã hoá để hiển thị (giải mã trong RAM, không ghi bản giải mã xuống đĩa). */
    fun openDecryptedStream(context: Context, path: String) =
        EncryptedFile.Builder(
            context, File(path), masterKey(context), EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileInput()

    /** Xoá hẳn 1 file ảnh (dùng khi dọn ảnh quá hạn lưu trữ). */
    fun deletePhoto(path: String) {
        runCatching { File(path).delete() }
    }
}
