package com.invsmart.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.invsmart.app.data.model.Store
import com.invsmart.app.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getStore(storeId: String): Result<Store?> = withContext(Dispatchers.IO) {
        runCatching {
            val doc = firestore.collection("stores").document(storeId).get().await()
            doc.toObject(Store::class.java)
        }
    }

    /**
     * This method is intended for App Admin to initialize a new store with its Master.
     */
    suspend fun initializeStore(storeName: String, masterUser: User): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val storeRef = firestore.collection("stores").document()
            val storeId = storeRef.id
            
            val batch = firestore.batch()
            
            val store = Store(
                storeId = storeId,
                name = storeName,
                masterUid = masterUser.uid,
                createdAt = Timestamp.now()
            )
            
            batch.set(storeRef, store)
            
            val userRef = firestore.collection("users").document(masterUser.uid)
            batch.set(userRef, masterUser.copy(
                storeId = storeId,
                roleGlobal = "master",
                isMaster = true,
                accessStatus = "active"
            ))
            
            batch.commit().await()
            storeId
        }
    }
}
