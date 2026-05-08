package com.invsmart.app.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.invsmart.app.databinding.FragmentUserSelectionBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.adapter.UserAdapter
import com.invsmart.app.ui.viewmodel.UserSelectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserSelectionFragment : Fragment() {

    private var _binding: FragmentUserSelectionBinding? = null
    private val binding get() = _binding!!
    
    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: UserSelectionViewModel by viewModels()
    private lateinit var adapter: UserAdapter

    // Arguments
    private var targetChainId: String = ""
    private var targetStoreId: String = ""
    private var targetRole: String = ""
    private var storeName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetChainId = it.getString("chainId") ?: ""
            targetStoreId = it.getString("storeId") ?: ""
            targetRole = it.getString("role") ?: "staff"
            storeName = it.getString("storeName") ?: "Cửa hàng"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Chọn người để mời"
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter { user ->
            showConfirmInviteDialog(user)
        }
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
    }

    private fun showConfirmInviteDialog(receiver: com.invsmart.app.data.model.User) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Gửi lời mời")
            .setMessage("Bạn có chắc chắn muốn mời ${receiver.email} vào vai trò ${targetRole.uppercase()} cho $storeName không?")
            .setPositiveButton("Gửi") { _, _ ->
                mainViewModel.uiState.value.currentUser?.let { sender ->
                    viewModel.sendInvitation(
                        sender = sender,
                        receiver = receiver,
                        chainId = targetChainId,
                        storeId = targetStoreId,
                        role = targetRole,
                        storeName = storeName
                    )
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.users.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.inviteStatus.collect { result ->
                        if (result != null) {
                            if (result.isSuccess) {
                                android.widget.Toast.makeText(requireContext(), "Đã gửi lời mời thành công!", android.widget.Toast.LENGTH_SHORT).show()
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            } else {
                                android.widget.Toast.makeText(requireContext(), "Lỗi: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            viewModel.clearStatus()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
