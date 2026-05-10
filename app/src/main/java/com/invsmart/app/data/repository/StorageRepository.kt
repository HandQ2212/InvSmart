package com.invsmart.app.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadImage(uri: Uri, folder: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = UUID.randomUUID().toString()
            val ref = storage.reference.child("$folder/$fileName")
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
        }
    }
}
