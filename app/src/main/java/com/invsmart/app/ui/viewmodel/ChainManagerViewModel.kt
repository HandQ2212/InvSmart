package com.invsmart.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invsmart.app.data.model.Chain
import com.invsmart.app.data.model.User
import com.invsmart.app.data.repository.ChainRepository
import com.invsmart.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChainManagerViewModel @Inject constructor(
    private val chainRepository: ChainRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _chains = MutableStateFlow<List<Chain>>(emptyList())
    val chains: StateFlow<List<Chain>> = _chains.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadChains()
    }

    fun loadChains() {
        viewModelScope.launch {
            chainRepository.getChains().collect {
                _chains.value = it
            }
        }
    }

    fun createChainAndMaster(chainName: String, masterEmail: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // B1: Tìm user theo email hoặc yêu cầu admin tạo user trước
            // Ở đây tôi giả định Admin nhập email của một User đã tồn tại 
            // Hoặc chúng ta có thể mở rộng logic để tự tạo Auth User (cần Firebase Admin SDK hoặc Cloud Functions)
            // Tạm thời tôi sẽ tìm User trong DB có email này
            userRepository.getAllUsers().onSuccess { users ->
                val masterUser = users.find { it.email.equals(masterEmail, ignoreCase = true) }
                if (masterUser != null) {
                    chainRepository.createChain(chainName, masterUser)
                        .onSuccess {
                            _message.value = "Đã tạo chuỗi $chainName và gán Master thành công"
                            loadChains()
                        }
                        .onFailure { _message.value = it.localizedMessage }
                } else {
                    _message.value = "Không tìm thấy User với email này. Hãy yêu cầu Master đăng ký tài khoản trước."
                }
            }
            _isLoading.value = false
        }
    }

    fun deleteChain(chainId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            chainRepository.deleteChain(chainId)
                .onSuccess {
                    _message.value = "Đã xóa chuỗi thành công"
                    loadChains()
                }
                .onFailure { _message.value = it.localizedMessage }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
