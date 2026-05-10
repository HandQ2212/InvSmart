package com.invsmart.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.invsmart.app.data.model.Store
import com.invsmart.app.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
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

    fun getStoresRealtime(chainId: String): Flow<Result<List<Store>>> = callbackFlow {
        val registration = firestore.collection("stores")
            .whereEqualTo("chainId", chainId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val stores = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Store::class.java)?.copy(storeId = doc.id)
                }.orEmpty()
                trySend(Result.success(stores))
            }
        awaitClose { registration.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun getStoresByChain(chainId: String): Result<List<Store>> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("stores")
                .whereEqualTo("chainId", chainId)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Store::class.java)?.copy(storeId = doc.id)
            }
        }
    }

    /**
     * This method is intended for Master to create a new branch and optionally assign a manager.
     */
    suspend fun createStore(store: Store, manager: User? = null): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val batch = firestore.batch()
            val storeRef = if (store.storeId.isEmpty()) {
                firestore.collection("stores").document()
            } else {
                firestore.collection("stores").document(store.storeId)
            }
            val storeId = storeRef.id
            
            val finalStore = store.copy(
                storeId = storeId,
                updatedAt = Timestamp.now()
            )
            batch.set(storeRef, finalStore)
            android.util.Log.d("STORE_REPO", "Batch: Setting store ${finalStore.name} with ID $storeId and ChainId ${finalStore.chainId}")

            if (manager != null) {
                val userRef = firestore.collection("users").document(manager.uid)
                android.util.Log.d("STORE_REPO", "Batch: Updating manager ${manager.email} to Store $storeId and Chain ${finalStore.chainId}")
                batch.update(userRef, mapOf(
                    "storeId" to storeId,
                    "chainId" to store.chainId,
                    "roleGlobal" to "manager",
                    "updatedAt" to Timestamp.now()
                ))
            }

            batch.commit().await()
            android.util.Log.d("STORE_REPO", "Batch commit SUCCESS for storeId: $storeId")
            storeId
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
                chainId = masterUser.chainId, // Added chainId mapping
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
                accessStatus = "active",
                updatedAt = Timestamp.now()
            ))
            
            batch.commit().await()
            storeId
        }
    }
}
