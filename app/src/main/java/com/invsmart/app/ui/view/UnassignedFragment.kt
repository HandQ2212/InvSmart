package com.invsmart.app.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.invsmart.app.R
import com.invsmart.app.databinding.FragmentUnassignedBinding
import com.invsmart.app.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.invsmart.app.ui.adapter.InvitationAdapter
import com.invsmart.app.ui.viewmodel.UnassignedViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UnassignedFragment : Fragment() {

    private var _binding: FragmentUnassignedBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: UnassignedViewModel by viewModels()
    private lateinit var adapter: InvitationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUnassignedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        mainViewModel.uiState.value.currentUser?.uid?.let {
            viewModel.loadInvitations(it)
        }
    }

    private fun setupRecyclerView() {
        adapter = InvitationAdapter(
            onAccept = { viewModel.respondToInvitation(it, true) },
            onReject = { viewModel.respondToInvitation(it, false) }
        )
        binding.rvInvitations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInvitations.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            mainViewModel.logout()
        }

        binding.swipeRefresh.setOnRefreshListener {
            mainViewModel.uiState.value.currentUser?.uid?.let {
                viewModel.loadInvitations(it)
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.invitations.collect { 
                        adapter.submitList(it)
                        binding.tvNoInvitations.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.operationResult.collect { result ->
                        if (result?.isSuccess == true) {
                            android.widget.Toast.makeText(requireContext(), "Thành công! Đang chuyển hướng...", android.widget.Toast.LENGTH_SHORT).show()
                            mainViewModel.checkAuthStatus()
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
