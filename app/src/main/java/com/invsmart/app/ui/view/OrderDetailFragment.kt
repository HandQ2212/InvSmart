package com.invsmart.app.ui.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.invsmart.app.R
import com.invsmart.app.databinding.FragmentOrderDetailBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.OrderDetailAdapter
import com.invsmart.app.util.VndFormatter
import com.invsmart.app.ui.viewmodel.StaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val staffViewModel: StaffViewModel by activityViewModels()
    
    private lateinit var detailAdapter: OrderDetailAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        detailAdapter = OrderDetailAdapter()
        binding.rvOrderDetails.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = detailAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        binding.btnConfirm.visibility = View.VISIBLE

        binding.rgPayment.setOnCheckedChangeListener { _, checkedId ->
            val method = if (checkedId == R.id.rbQr) "qr" else "cash"
            staffViewModel.setPaymentMethod(method)
            binding.btnScanQr.visibility = if (method == "qr") View.VISIBLE else View.GONE
        }

        binding.btnScanQr.setOnClickListener {
            // Simulate PayOS QR Payment
            showQrPaymentDialog()
        }

        binding.btnConfirm.setOnClickListener {
            val method = staffViewModel.paymentMethod.value
            if (method == "cash") {
                showCashConfirmationDialog()
            } else {
                processSubmitOrder()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    staffViewModel.isPaymentConfirmed.collect { isConfirmed ->
                        binding.btnConfirm.isEnabled = isConfirmed
                        // Visual feedback for disabled state
                        binding.btnConfirm.alpha = if (isConfirmed) 1.0f else 0.5f
                    }
                }
                
                launch {
                    staffViewModel.selectedProducts.collect { selections ->
                        val list = selections.toList()
                        detailAdapter.submitList(list)
                        binding.tvTotalQuantity.text = selections.values.sum().toString()
                        val totalAmount = selections.entries.sumOf { (product, quantity) ->
                            product.price.toDouble() * quantity
                        }
                        binding.tvTotalAmount.text = VndFormatter.format(totalAmount)
                    }
                }

                launch {
                    staffViewModel.orderCreationState.collect { result ->
                        if (result != null) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnConfirm.isEnabled = true
                            if (result.isSuccess) {
                                val created = result.getOrNull()
                                if (created != null) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Đã tạo phiếu ${created.orderId}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                staffViewModel.clearSelections()
                                findNavController().navigate(R.id.action_orderDetailFragment_to_staffHomeFragment)
                            } else {
                                val err = result.exceptionOrNull()?.message ?: "Lỗi tạo đơn"
                                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showCashConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận thanh toán")
            .setMessage("Xác nhận khách hàng đã thanh toán bằng tiền mặt?")
            .setPositiveButton("Xác nhận") { _, _ ->
                processSubmitOrder()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showQrPaymentDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_payment, null)
        
        val totalAmount = binding.tvTotalAmount.text.toString()
        dialogView.findViewById<TextView>(R.id.tvQrAmount).text = totalAmount

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancelPayment).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnSimulateSuccess).setOnClickListener {
            staffViewModel.setPaymentConfirmed(true)
            Toast.makeText(requireContext(), "Thanh toán QR thành công!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun processSubmitOrder() {
        val user = mainViewModel.uiState.value.currentUser
        if (user != null) {
            binding.btnConfirm.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
            staffViewModel.submitOrder(
                currentUser = user,
                orderType = "sale"
            )
        } else {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy tài khoản", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}