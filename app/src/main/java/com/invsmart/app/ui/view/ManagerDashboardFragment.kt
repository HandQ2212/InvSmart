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
import androidx.navigation.fragment.findNavController
import com.invsmart.app.R
import com.invsmart.app.databinding.FragmentManagerDashboardBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.viewmodel.ManagerViewModel
import com.invsmart.app.util.VndFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerDashboardFragment : Fragment() {

    private var _binding: FragmentManagerDashboardBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()
    private var lastLoadedRoleKey: String? = null

    private fun applyRoleHeader(role: String) {
        if (role == "master") {
            binding.toolbar.title = "Master Dashboard"
            binding.tvWelcome.text = "Xin chào, Master!"
            binding.layoutMasterActions.visibility = View.VISIBLE
        } else {
            binding.toolbar.title = "Manager Dashboard"
            binding.tvWelcome.text = "Xin chào, Quản lý!"
            binding.layoutMasterActions.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel.uiState.value.currentUser?.let {
            lastLoadedRoleKey = "${it.uid}:${it.roleGlobal}"
            applyRoleHeader(it.roleGlobal)
            managerViewModel.loadDashboard(it)
        }

        setupRevenueList()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    managerViewModel.revenue.collect { revenue ->
                        binding.tvRevenue.text = VndFormatter.format(revenue)
                    }
                }
                launch {
                    managerViewModel.managerMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            managerViewModel.resetMessage()
                        }
                    }
                }
                launch {
                    mainViewModel.uiState.collect { state ->
                        val actor = state.currentUser ?: return@collect
                        val roleKey = "${actor.uid}:${actor.roleGlobal}"
                        if (lastLoadedRoleKey != roleKey) {
                            lastLoadedRoleKey = roleKey
                            applyRoleHeader(actor.roleGlobal)
                            managerViewModel.loadDashboard(actor)
                        }
                    }
                }
            }
        }

        binding.cardInventory.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_inventory)
        }

        binding.cardStaff.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_staffManager)
        }

        binding.cardBranch.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_branchManager)
        }

        binding.cardCatalog.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_catalogManager)
        }

        binding.btnLogout.setOnClickListener {
            mainViewModel.logout()
            findNavController().navigate(R.id.action_global_logout)
        }
    }

    private fun setupRevenueList() {
        val adapter = BranchRevenueAdapter()
        binding.rvBranchRevenue.adapter = adapter
        binding.rvBranchRevenue.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                managerViewModel.stores.collect { stores ->
                    val revenueMap = managerViewModel.branchRevenueMap.value
                    adapter.submitData(stores, revenueMap)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                managerViewModel.branchRevenueMap.collect { revenueMap ->
                    val stores = managerViewModel.stores.value
                    adapter.submitData(stores, revenueMap)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
