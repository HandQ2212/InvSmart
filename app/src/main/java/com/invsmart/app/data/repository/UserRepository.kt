package com.invsmart.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QueryDocumentSnapshot
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
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun normalizeRole(roleGlobal: String?, roleLegacy: String?, isMaster: Boolean): String {
        val global = roleGlobal?.trim()?.lowercase().orEmpty()
        val legacy = roleLegacy?.trim()?.lowercase().orEmpty()
        
        return when {
            global == "admin" || legacy == "admin" -> "admin"
            isMaster || global == "master" -> "master"
            global == "manager" || legacy == "manager" -> "manager"
            global == "unassigned" -> "unassigned"
            else -> "staff"
        }
    }

    private fun mapUser(doc: DocumentSnapshot): User {
        val isMaster = doc.getBoolean("isMaster") ?: false
        val normalizedRole = normalizeRole(
            roleGlobal = doc.getString("roleGlobal"),
            roleLegacy = doc.getString("role"),
            isMaster = isMaster
        )

        return User(
            uid = doc.getString("uid") ?: doc.id,
            username = doc.getString("username") ?: "",
            usernameLower = doc.getString("usernameLower") ?: "",
            email = doc.getString("email") ?: "",
            fullName = doc.getString("fullName") ?: "",
            role = doc.getString("role") ?: "",
            roleGlobal = normalizedRole,
            isMaster = isMaster || normalizedRole == "master",
            chainId = doc.getString("chainId") ?: "",
            storeId = doc.getString("storeId") ?: doc.getString("defaultTeamId") ?: "",
            status = doc.getString("status") ?: "active",
            defaultTeamId = doc.getString("defaultTeamId"),
            accessStatus = doc.getString("accessStatus") ?: "pending",
            phoneNumber = doc.getString("phoneNumber") ?: "",
            createdAt = doc.getTimestamp("createdAt") ?: com.google.firebase.Timestamp.now(),
            updatedAt = doc.getTimestamp("updatedAt")
        )
    }

    private fun mapUser(doc: QueryDocumentSnapshot): User {
        return mapUser(doc as DocumentSnapshot)
    }

    suspend fun getUser(uid: String): Result<User?> = withContext(Dispatchers.IO) {
        runCatching {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                mapUser(document)
            } else {
                null
            }
        }
    }

    suspend fun createUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("users").document(user.uid).set(user).await()
            Unit
        }
    }

    suspend fun getAllUsers(): Result<List<User>> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("users").get().await()
            snapshot.documents
                .filterIsInstance<QueryDocumentSnapshot>()
                .map { mapUser(it) }
        }
    }

    suspend fun getAllStaffUsers(): Result<List<User>> = withContext(Dispatchers.IO) {
        getAllUsers().map { users ->
            users.filter { user -> user.roleGlobal == "staff" }
        }
    }

    suspend fun getManageableUsers(actor: User): Result<List<User>> = withContext(Dispatchers.IO) {
        runCatching {
            val baseQuery = firestore.collection("users")
            val query = when (actor.roleGlobal) {
                "admin" -> baseQuery // Admin sees everyone (but logic below filters)
                "master" -> {
                    if (actor.chainId.isEmpty()) baseQuery.whereEqualTo("uid", "none")
                    else baseQuery.whereEqualTo("chainId", actor.chainId)
                }
                "manager" -> baseQuery.whereEqualTo("storeId", actor.storeId)
                else -> baseQuery.whereEqualTo("uid", "none")
            }
            val snapshot = query.get().await()
            
            val users = snapshot.documents.map { mapUser(it) }
            val actorRole = actor.roleGlobal
            
            users
                .filter { it.uid != actor.uid }
                .filter { target ->
                    when (actorRole) {
                "admin" -> target.roleGlobal == "master" || target.roleGlobal == "unassigned"
                        "master" -> target.roleGlobal == "manager" || target.roleGlobal == "staff" || target.roleGlobal == "unassigned"
                        "manager" -> target.roleGlobal == "staff"
                        else -> false
                    }
                }
                .sortedWith(compareBy<User> { it.roleGlobal }.thenBy { it.email })
        }
    }

    suspend fun updateUserRole(uid: String, newRole: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedRole = when (newRole.trim().lowercase()) {
                "master" -> "master"
                "manager" -> "manager"
                else -> "staff"
            }

            firestore.collection("users").document(uid).update(
                mapOf(
                    "roleGlobal" to normalizedRole,
                    "role" to normalizedRole,
                    "isMaster" to (normalizedRole == "master"),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            Unit
        }
    }

    fun getUnassignedUsers(): Flow<List<User>> = firestore.collection("users")
        .snapshots()
        .map { snapshot ->
            snapshot.documents
                .map { mapUser(it) }
                .filter { it.roleGlobal != "admin" }
        }

    suspend fun updateProfile(uid: String, fullName: String, phoneNumber: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "fullName" to fullName,
                    "phoneNumber" to phoneNumber,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            Unit
        }
    }
}
