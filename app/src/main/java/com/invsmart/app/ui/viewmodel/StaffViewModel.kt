package com.invsmart.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invsmart.app.data.model.Order
import com.invsmart.app.data.model.OrderItem
import com.invsmart.app.data.model.Product
import com.invsmart.app.data.model.User
import com.invsmart.app.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    data class CreatedOrderInfo(
        val orderId: String,
        val totalAmount: Double
    )

    private val _selectedProducts = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val selectedProducts: StateFlow<Map<Product, Int>> = _selectedProducts.asStateFlow()

    private val _orderCreationState = MutableStateFlow<Result<CreatedOrderInfo>?>(null)
    val orderCreationState: StateFlow<Result<CreatedOrderInfo>?> = _orderCreationState.asStateFlow()

    fun setProductQuantity(product: Product, quantity: Int) {
        _selectedProducts.update { currentMap ->
            val mutableMap = currentMap.toMutableMap()
            if (quantity <= 0) {
                mutableMap.remove(product)
            } else {
                mutableMap[product] = quantity
            }
            mutableMap
        }
    }

    fun clearSelections() {
        _selectedProducts.value = emptyMap()
        _orderCreationState.value = null
    }

    private val _paymentMethod = MutableStateFlow("cash") // "cash" or "qr"
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _isPaymentConfirmed = MutableStateFlow(false)
    val isPaymentConfirmed: StateFlow<Boolean> = _isPaymentConfirmed.asStateFlow()

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        // If switching back to cash, it's considered "confirmed" for UI purposes
        _isPaymentConfirmed.value = (method == "cash")
    }

    fun setPaymentConfirmed(confirmed: Boolean) {
        _isPaymentConfirmed.value = confirmed
    }

    fun submitOrder(currentUser: User, orderType: String = "sale") {
        viewModelScope.launch {
            val selections = _selectedProducts.value
            if (selections.isEmpty()) return@launch

            val orderItems = selections.map { (product, quantity) ->
                OrderItem(
                    productId = product.productId.ifBlank { product.sku },
                    productName = product.name,
                    quantity = quantity,
                    priceAtTime = product.price
                )
            }

            val totalQty = selections.values.sum()
            val totalAmount = selections.entries.sumOf { (product, qty) -> product.price * qty }

            val order = Order(
                orderId = "",
                chainId = currentUser.chainId,
                storeId = currentUser.storeId,
                staffUid = currentUser.uid,
                staffName = currentUser.fullName,
                orderType = orderType,
                items = orderItems,
                totalQuantity = totalQty,
                totalAmount = totalAmount,
                paymentMethod = _paymentMethod.value,
                status = if (_paymentMethod.value == "qr") "paid" else "pending_payment"
            )

            val result = orderRepository.createOrder(order)
            _orderCreationState.value = result.map { orderId ->
                CreatedOrderInfo(orderId = orderId, totalAmount = totalAmount)
            }

            if (result.isSuccess) {
                _selectedProducts.value = emptyMap()
                _isPaymentConfirmed.value = false
            }
        }
    }
}