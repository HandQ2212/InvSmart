package com.invsmart.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.invsmart.app.data.model.TeamInvite
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
class TeamRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getManagedTeamId(managerUid: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("team_members")
                .whereEqualTo("uid", managerUid)
                .whereEqualTo("teamRole", "manager")
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.getString("teamId")
        }
    }

    suspend fun getActiveTeamId(uid: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("team_members")
                .whereEqualTo("uid", uid)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.getString("teamId")
        }
    }

    suspend fun createInvite(managerUid: String, staffEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedEmail = staffEmail.trim().lowercase()
            require(normalizedEmail.isNotEmpty()) { "Email không hợp lệ" }

            val teamId = getManagedTeamId(managerUid).getOrThrow()
                ?: throw IllegalStateException("Không tìm thấy team quản lý")

            val now = Timestamp.now()
            val inviteRef = firestore.collection("team_invites").document()

            val batch = firestore.batch()
            batch.set(inviteRef, mapOf(
                "inviteId" to inviteRef.id,
                "teamId" to teamId,
                "managerUid" to managerUid,
                "staffUid" to "",
                "staffEmail" to normalizedEmail,
                "status" to "pending",
                "token" to "invite_${inviteRef.id}",
                "createdAt" to now,
                "expiresAt" to Timestamp(now.seconds + 7L * 24L * 60L * 60L, now.nanoseconds),
                "respondedAt" to null
            ))

            batch.commit().await()
            Unit
        }.recoverCatching { throwable ->
            if (throwable is FirebaseFirestoreException && throwable.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw IllegalStateException("Bạn không có quyền gửi lời mời. Hãy đăng nhập tài khoản manager hoặc cập nhật Firestore Rules mới nhất.")
            }
            throw throwable
        }
    }

    suspend fun getPendingInvitesForStaff(uid: String, email: String): Result<List<TeamInvite>> = withContext(Dispatchers.IO) {
        runCatching {
            val byUid = firestore.collection("team_invites")
                .whereEqualTo("staffUid", uid)
                .whereEqualTo("status", "pending")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(TeamInvite::class.java) }

            val normalizedEmail = email.trim().lowercase()
            val byEmail = if (normalizedEmail.isNotEmpty()) {
                firestore.collection("team_invites")
                    .whereEqualTo("staffEmail", normalizedEmail)
                    .whereEqualTo("status", "pending")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(TeamInvite::class.java) }
            } else {
                emptyList()
            }

            (byUid + byEmail)
                .associateBy { it.inviteId }
                .values
                .sortedByDescending { it.createdAt }
        }
    }

    fun observePendingInvitesForStaff(uid: String, email: String): Flow<Result<List<TeamInvite>>> = callbackFlow {
        val normalizedEmail = email.trim().lowercase()
        var byUidInvites: List<TeamInvite> = emptyList()
        var byEmailInvites: List<TeamInvite> = emptyList()

        fun emitCombined() {
            val merged = (byUidInvites + byEmailInvites)
                .associateBy { it.inviteId }
                .values
                .sortedByDescending { it.createdAt }
            trySend(Result.success(merged))
        }

        val registrations = mutableListOf<ListenerRegistration>()

        val byUidRegistration = firestore.collection("team_invites")
            .whereEqualTo("staffUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                byUidInvites = snapshot?.documents?.mapNotNull { it.toObject(TeamInvite::class.java) }.orEmpty()
                emitCombined()
            }
        registrations.add(byUidRegistration)

        if (normalizedEmail.isNotEmpty()) {
            val byEmailRegistration = firestore.collection("team_invites")
                .whereEqualTo("staffEmail", normalizedEmail)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    byEmailInvites = snapshot?.documents?.mapNotNull { it.toObject(TeamInvite::class.java) }.orEmpty()
                    emitCombined()
                }
            registrations.add(byEmailRegistration)
        }

        awaitClose {
            registrations.forEach { it.remove() }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun respondInvite(invite: TeamInvite, staffUid: String, accept: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(invite.inviteId.isNotEmpty()) { "Invite không hợp lệ" }
            require(invite.teamId.isNotEmpty()) { "Thiếu teamId" }

            val now = Timestamp.now()
            val membershipRef = firestore.collection("team_members").document("${invite.teamId}_${staffUid}")
            val inviteRef = firestore.collection("team_invites").document(invite.inviteId)
            val userRef = firestore.collection("users").document(staffUid)

            val newStatus = if (accept) "accepted" else "rejected"
            val memberStatus = if (accept) "active" else "removed"

            val batch = firestore.batch()
            batch.update(inviteRef, mapOf(
                "status" to newStatus,
                "respondedAt" to now,
                "staffUid" to staffUid
            ))
            batch.set(membershipRef, mapOf(
                "teamId" to invite.teamId,
                "uid" to staffUid,
                "teamRole" to "staff",
                "status" to memberStatus,
                "updatedAt" to now,
                "joinedAt" to if (accept) now else null
            ), com.google.firebase.firestore.SetOptions.merge())

            if (accept) {
                batch.set(userRef, mapOf(
                    "defaultTeamId" to invite.teamId,
                    "accessStatus" to "active",
                    "updatedAt" to now
                ), com.google.firebase.firestore.SetOptions.merge())
            }

            batch.commit().await()
            Unit
        }
    }

    suspend fun findUserByEmail(email: String): Result<User?> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = email.trim().lowercase()
            if (normalized.isEmpty()) return@runCatching null
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", normalized)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(User::class.java)
        }
    }
}