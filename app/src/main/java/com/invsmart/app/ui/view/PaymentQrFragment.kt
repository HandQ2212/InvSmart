package com.invsmart.app.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.invsmart.app.R
import com.invsmart.app.data.repository.OrderRepository
import com.invsmart.app.databinding.FragmentPaymentQrBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PaymentQrFragment : Fragment() {

    private var _binding: FragmentPaymentQrBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var orderRepository: OrderRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentQrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = arguments?.getString("orderId").orEmpty()
        val totalAmount = arguments?.getDouble("totalAmount") ?: 0.0

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.tvOrderId.text = "Mã đơn: $orderId"
        binding.tvAmount.text = "Tổng thanh toán: %,.0f đ".format(totalAmount)

        val qrPayload = "INVSMART|$orderId|${"%.0f".format(totalAmount)}"
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=600x600&data=$qrPayload"

        Glide.with(this)
            .load(qrUrl)
            .into(binding.ivQr)

        binding.btnPaid.setOnClickListener {
            if (orderId.isBlank()) {
                Toast.makeText(requireContext(), "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnPaid.isEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                orderRepository.markOrderPaid(orderId)
                    .onSuccess {
                        Toast.makeText(requireContext(), "Thanh toán thành công", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_paymentQrFragment_to_staffHomeFragment)
                    }
                    .onFailure {
                        binding.btnPaid.isEnabled = true
                        Toast.makeText(requireContext(), it.localizedMessage ?: "Không thể cập nhật thanh toán", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
