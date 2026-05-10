package com.invsmart.app.ui.view

import com.invsmart.app.data.model.Store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.invsmart.app.R
import com.invsmart.app.data.model.User
import com.invsmart.app.databinding.FragmentBranchManagementBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.adapter.BranchRevenueAdapter
import com.invsmart.app.ui.viewmodel.ManagerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BranchManagementFragment : Fragment() {

    private var _binding: FragmentBranchManagementBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()
    
    private lateinit var adapter: BranchRevenueAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBranchManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        adapter = BranchRevenueAdapter()
        binding.rvBranches.adapter = adapter
        binding.rvBranches.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                managerViewModel.stores.collect { stores ->
                    adapter.submitData(stores, managerViewModel.branchRevenueMap.value)
                }
            }
        }

        binding.btnAddBranch.setOnClickListener {
            showAddBranchDialog()
        }
    }

    private fun showAddBranchDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_branch, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etStoreName)
        val etAddress = dialogView.findViewById<TextInputEditText>(R.id.etStoreAddress)
        val spinnerManager = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerManager)

        val unassignedUsers = managerViewModel.managedUsers.value.filter { it.roleGlobal == "unassigned" || it.roleGlobal == "staff" }
        val userNames = unassignedUsers.map { "${it.fullName} (${it.email})" }
        val adapterManagers = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, userNames)
        spinnerManager.setAdapter(adapterManagers)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Thêm chi nhánh mới")
            .setView(dialogView)
            .setPositiveButton("Tạo") { _, _ ->
                val name = etName.text.toString()
                val address = etAddress.text.toString()
                val managerIdx = userNames.indexOf(spinnerManager.text.toString())
                val selectedManager = if (managerIdx >= 0) unassignedUsers[managerIdx] else null
                
                android.util.Log.d("BRANCH_MGMT", "Attempting to create branch: $name, address: $address, manager: ${selectedManager?.email ?: "none"}")
                
                if (name.isNotEmpty()) {
                    mainViewModel.uiState.value.currentUser?.let { actor ->
                        android.util.Log.d("BRANCH_MGMT", "Actor: ${actor.uid}, ChainId: ${actor.chainId}")
                        managerViewModel.createBranch(name, address, actor, selectedManager)
                    }
                } else {
                    android.util.Log.w("BRANCH_MGMT", "Branch name is empty!")
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
