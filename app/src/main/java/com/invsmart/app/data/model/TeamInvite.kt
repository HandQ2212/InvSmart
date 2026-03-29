package com.invsmart.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class TeamInvite(
    @DocumentId
    val inviteId: String = "",
    val teamId: String = "",
    val managerUid: String = "",
    val staffUid: String = "",
    val staffEmail: String = "",
    val status: String = "pending",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val respondedAt: Timestamp? = null
)

data class TeamMember(
    @DocumentId
    val membershipId: String = "",
    val teamId: String = "",
    val uid: String = "",
    val teamRole: String = "staff",
    val status: String = "pending",
    val joinedAt: Timestamp? = null,
    val invitedBy: String = "",
    val updatedAt: Timestamp? = null
)