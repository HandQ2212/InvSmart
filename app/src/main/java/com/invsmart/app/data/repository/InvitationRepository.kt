package com.invsmart.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.invsmart.app.data.model.Invitation
import com.invsmart.app.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvitationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) {
    suspend fun sendInvitation(invitation: Invitation): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ref = firestore.collection("invitations").document()
            firestore.collection("invitations").document(ref.id)
                .set(invitation.copy(id = ref.id)).await()
            Unit
        }
    }

    suspend fun getInvitationsForUser(uid: String): Result<List<Invitation>> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("invitations")
                .whereEqualTo("receiverUid", uid)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Invitation::class.java) }
        }
    }

    suspend fun respondToInvitation(invitation: Invitation, accept: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val batch = firestore.batch()
            val invRef = firestore.collection("invitations").document(invitation.id)
            
            if (accept) {
                batch.update(invRef, "status", "accepted")
                
                // Update the user profile
                val userRef = firestore.collection("users").document(invitation.receiverUid)
                batch.update(userRef, mapOf(
                    "roleGlobal" to invitation.targetRole,
                    "chainId" to invitation.targetChainId,
                    "storeId" to invitation.targetStoreId,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                ))
            } else {
                batch.update(invRef, "status", "rejected")
            }
            
            batch.commit().await()
            Unit
        }
    }
}
