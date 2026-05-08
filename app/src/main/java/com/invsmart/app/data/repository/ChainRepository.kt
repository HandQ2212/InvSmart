package com.invsmart.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.invsmart.app.data.model.Chain
import com.invsmart.app.data.model.Store
import com.invsmart.app.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChainRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createChain(name: String, masterUser: User): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val chainRef = firestore.collection("chains").document()
            val chainId = chainRef.id
            
            val batch = firestore.batch()
            
            val chain = Chain(
                chainId = chainId,
                name = name,
                masterUid = masterUser.uid,
                createdAt = Timestamp.now()
            )
            
            batch.set(chainRef, chain)
            
            // Update master user with chainId
            val userRef = firestore.collection("users").document(masterUser.uid)
            batch.set(userRef, masterUser.copy(
                chainId = chainId,
                roleGlobal = "master",
                isMaster = true,
                accessStatus = "active"
            ))
            
            batch.commit().await()
            chainId
        }
    }

    fun getChains(): Flow<List<Chain>> = firestore.collection("chains")
        .snapshots()
        .map { snapshot ->
            snapshot.documents.mapNotNull { it.toObject(Chain::class.java) }
        }

    suspend fun deleteChain(chainId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("chains").document(chainId).delete().await()
            Unit
        }
    }

    suspend fun assignMasterToChain(chainId: String, masterUser: User): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val batch = firestore.batch()
            
            // 1. Update Chain's masterUid
            val chainRef = firestore.collection("chains").document(chainId)
            batch.update(chainRef, "masterUid", masterUser.uid)
            
            // 2. Update User's chainId and role
            val userRef = firestore.collection("users").document(masterUser.uid)
            batch.update(userRef, mapOf(
                "chainId" to chainId,
                "roleGlobal" to "master",
                "isMaster" to true,
                "accessStatus" to "active"
            ))
            
            batch.commit().await()
            Unit
        }
    }
}
