package com.invsmart.app.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.invsmart.app.R
import com.invsmart.app.data.repository.OrderRepository
import com.invsmart.app.databinding.FragmentStaffHomeBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.OrderAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StaffHomeFragment : Fragment() {

    private var _binding: FragmentStaffHomeBinding? = null
    private val binding get() = _binding!!
    
    private val mainViewModel: MainViewModel by activityViewModels()
    private val orderAdapter = OrderAdapter()

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

        loadOrders()
    }

    private fun loadOrders() {
        val user = mainViewModel.uiState.value.currentUser
        if (user != null) {
            binding.progressBar.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                orderRepository.getOrdersByStaff(user.uid).onSuccess { orders ->
                    binding.progressBar.visibility = View.GONE
                    orderAdapter.submitList(orders.sortedByDescending { it.createdAt })
                }.onFailure {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Lỗi tải danh sách", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}