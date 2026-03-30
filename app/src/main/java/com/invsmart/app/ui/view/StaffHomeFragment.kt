package com.invsmart.app.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.invsmart.app.R
import com.invsmart.app.data.repository.OrderRepository
import com.invsmart.app.databinding.FragmentStaffHomeBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.OrderAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StaffHomeFragment : Fragment() {

    private var _binding: FragmentStaffHomeBinding? = null
    private val binding get() = _binding!!
    
    private val mainViewModel: MainViewModel by activityViewModels()
    private val orderAdapter = OrderAdapter { order ->
        findNavController().navigate(
            R.id.action_staffHomeFragment_to_orderInvoiceDetailFragment,
            bundleOf("orderId" to order.orderId)
        )
    }
    private var lastLoadedStaffUid: String? = null

    @Inject
    lateinit var orderRepository: OrderRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.inflateMenu(R.menu.staff_menu)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_profile -> {
                    findNavController().navigate(R.id.action_staffHomeFragment_to_staffAccountFragment)
                    true
                }
                R.id.action_logout -> {
                    mainViewModel.logout()
                    findNavController().navigate(R.id.action_global_logout)
                    true
                }
                else -> false
            }
        }

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }

        binding.fabNewOrder.setOnClickListener {
            findNavController().navigate(R.id.action_staffHomeFragment_to_productListFragment)
        }

        observeUserAndLoadOrders()
    }

    override fun onResume() {
        super.onResume()
        // Refresh when user returns to app to keep invoice list up to date.
        mainViewModel.uiState.value.currentUser?.uid?.let { uid ->
            loadOrders(uid)
        }
    }

    private fun observeUserAndLoadOrders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.uiState
                        .map { it.currentUser?.uid }
                        .collect { uid ->
                            if (!uid.isNullOrBlank() && uid != lastLoadedStaffUid) {
                                lastLoadedStaffUid = uid
                                loadOrders(uid)
                            }
                        }
                }
            }
        }
    }

    private fun loadOrders(staffUid: String) {
        binding.progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            orderRepository.getOrdersByStaff(staffUid).onSuccess { orders ->
                binding.progressBar.visibility = View.GONE
                orderAdapter.submitList(orders.sortedByDescending { it.createdAt })
            }.onFailure {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Lỗi tải danh sách", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}