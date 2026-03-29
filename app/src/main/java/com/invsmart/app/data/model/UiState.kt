package com.invsmart.app.data.model

data class UiState(
    val authState: AuthState = AuthState.Unauthenticated,
    val products: List<Product> = emptyList(),
    val isLoadingProducts: Boolean = false,
    val message: String = "",
    val currentUser: User? = null,
    val activeTeamId: String? = null
)
