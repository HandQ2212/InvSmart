package com.invsmart.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.invsmart.app.data.model.Invitation
import com.invsmart.app.data.model.User
import com.invsmart.app.data.repository.ChainRepository
import com.invsmart.app.data.repository.InvitationRepository
import com.invsmart.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSelectionViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val invitationRepository: InvitationRepository,
    private val chainRepository: ChainRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inviteStatus = MutableStateFlow<Result<Unit>?>(null)
    val inviteStatus: StateFlow<Result<Unit>?> = _inviteStatus.asStateFlow()

    init {
        loadUnassignedUsers()
    }

    fun loadUnassignedUsers() {
        viewModelScope.launch {
            userRepository.getUnassignedUsers().collect {
                _users.value = it
            }
        }
    }

    fun assignMasterDirectly(chainId: String, receiver: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _inviteStatus.value = chainRepository.assignMasterToChain(chainId, receiver)
            _isLoading.value = false
        }
    }

    fun sendInvitation(
        sender: User,
        receiver: User,
        chainId: String,
        storeId: String,
        role: String,
        storeName: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Nếu là Master và Admin đang gán trực tiếp (Direct Assignment)
            if (role == "master" && chainId.isNotEmpty()) {
                _inviteStatus.value = chainRepository.assignMasterToChain(chainId, receiver)
                _isLoading.value = false
                return@launch
            }

            var finalChainId = chainId
            // Nếu là Master và chưa có chainId, tức là tạo mới Chuỗi
            if (role == "master" && chainId.isEmpty()) {
                val chainResult = chainRepository.createChain(storeName, receiver)
                chainResult.onSuccess { finalChainId = it }
                    .onFailure { 
                        _inviteStatus.value = Result.failure(it)
                        _isLoading.value = false
                        return@launch 
                    }
            }

            android.util.Log.d("INVITE", "Preparing invitation: From=${sender.email}, To=${receiver.email}, Role=$role, Chain=$finalChainId")
            val invitation = Invitation(
                senderUid = sender.uid,
                senderName = sender.fullName.ifEmpty { sender.email },
                receiverUid = receiver.uid,
                targetChainId = finalChainId,
                targetStoreId = storeId,
                targetRole = role,
                storeName = storeName,
                createdAt = Timestamp.now()
            )
            
            android.util.Log.d("INVITE", "Calling Repository.sendInvitation...")
            val result = invitationRepository.sendInvitation(invitation)
            _inviteStatus.value = result
            
            if (result.isSuccess) {
                android.util.Log.d("INVITE", "SUCCESS: Invitation sent to ${receiver.email}")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                android.util.Log.e("INVITE", "FAILED: Invitation to ${receiver.email} error: $error")
            }
            _isLoading.value = false
        }
    }

    fun clearStatus() {
        _inviteStatus.value = null
    }
}
