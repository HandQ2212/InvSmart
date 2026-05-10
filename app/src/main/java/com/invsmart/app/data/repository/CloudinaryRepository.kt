package com.invsmart.app.data.repository

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.invsmart.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CloudinaryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        try {
            val config = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "secure" to true
            )
            MediaManager.init(context, config)
        } catch (e: Exception) {
            // MediaManager already initialized or other error
        }
    }

    suspend fun uploadImage(uri: Uri): Result<String> = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val url = resultData?.get("secure_url") as? String
                    if (url != null) {
                        continuation.resume(Result.success(url))
                    } else {
                        continuation.resume(Result.failure(Exception("Cloudinary upload success but URL is null")))
                    }
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resume(Result.failure(Exception(error?.description ?: "Unknown Cloudinary error")))
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    continuation.resume(Result.failure(Exception("Upload rescheduled/failed")))
                }
            })
            .dispatch()
    }
}
