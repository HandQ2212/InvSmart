package com.invsmart.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invsmart.app.data.model.Invitation
import com.invsmart.app.data.repository.InvitationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnassignedViewModel @Inject constructor(
    private val invitationRepository: InvitationRepository
) : ViewModel() {

    private val _invitations = MutableStateFlow<List<Invitation>>(emptyList())
    val invitations: StateFlow<List<Invitation>> = _invitations.asStateFlow()

    private val _operationResult = MutableStateFlow<Result<Unit>?>(null)
    val operationResult: StateFlow<Result<Unit>?> = _operationResult.asStateFlow()

    fun loadInvitations(uid: String) {
        viewModelScope.launch {
            invitationRepository.getInvitationsForUser(uid)
                .onSuccess { _invitations.value = it }
        }
    }

    fun respondToInvitation(invitation: Invitation, accept: Boolean) {
        viewModelScope.launch {
            val result = invitationRepository.respondToInvitation(invitation, accept)
            _operationResult.value = result
            if (result.isSuccess) {
                loadInvitations(invitation.receiverUid)
            }
        }
    }

    fun clearResult() {
        _operationResult.value = null
    }
}
