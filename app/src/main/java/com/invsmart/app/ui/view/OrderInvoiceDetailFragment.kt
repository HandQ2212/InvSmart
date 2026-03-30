package com.invsmart.app.ui.view

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.invsmart.app.data.model.Order
import com.invsmart.app.data.repository.OrderRepository
import com.invsmart.app.databinding.FragmentOrderInvoiceDetailBinding
import com.invsmart.app.ui.OrderItemHistoryAdapter
import com.invsmart.app.util.VndFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OrderInvoiceDetailFragment : Fragment() {

    private var _binding: FragmentOrderInvoiceDetailBinding? = null
    private val binding get() = _binding!!

    private val orderItemAdapter = OrderItemHistoryAdapter()

    @Inject
    lateinit var orderRepository: OrderRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderInvoiceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.rvOrderItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderItemAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        val orderId = arguments?.getString("orderId").orEmpty()
        if (orderId.isBlank()) {
            Toast.makeText(requireContext(), "Thiếu mã đơn hàng", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        loadOrderDetail(orderId)
    }

    private fun loadOrderDetail(orderId: String) {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            orderRepository.getOrderById(orderId)
                .onSuccess { order ->
                    binding.progressBar.visibility = View.GONE
                    bindOrder(order)
                }
                .onFailure { error ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        error.localizedMessage ?: "Không tải được chi tiết hóa đơn",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
                }
        }
    }

    private fun bindOrder(order: Order) {
        binding.tvOrderCode.text = "Mã đơn: ${order.orderId}"
        binding.tvOrderType.text = "Loại: ${if (order.orderType == "import") "Nhập kho" else "Xuất kho"}"
        binding.tvOrderDate.text = "Ngày: ${DateFormat.format("dd/MM/yyyy HH:mm", order.createdAt.toDate())}"
        binding.tvOrderStatus.text = "Trạng thái: ${mapStatus(order.status)}"

        orderItemAdapter.submitList(order.items)
        binding.tvTotalQuantity.text = order.totalQuantity.toString()
        binding.tvTotalAmount.text = VndFormatter.format(order.totalAmount)
    }

    private fun mapStatus(status: String): String {
        return when (status) {
            "pending_payment" -> "Chờ thanh toán"
            "paid" -> "Đã thanh toán"
            else -> status
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
