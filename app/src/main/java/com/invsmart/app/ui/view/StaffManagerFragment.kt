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
import com.invsmart.app.data.model.User
import androidx.recyclerview.widget.LinearLayoutManager
import com.invsmart.app.databinding.FragmentStaffManagerBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.UserAdapter
import com.invsmart.app.ui.viewmodel.ManagerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffManagerFragment : Fragment() {

    private var _binding: FragmentStaffManagerBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()
    private lateinit var userAdapter: UserAdapter
    private var actorUser: User? = null
    private var lastLoadedRoleKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        userAdapter = UserAdapter(
            canToggleRole = { user ->
                actorUser?.roleGlobal == "master" && user.roleGlobal != "master"
            },
            onChangeRoleClick = { user ->
                val actor = actorUser
                if (actor == null) {
                    Toast.makeText(requireContext(), "Không lấy được thông tin tài khoản hiện tại", Toast.LENGTH_SHORT).show()
                } else {
                    managerViewModel.toggleManagerStaffRole(actor, user)
                }
            }
        )

        binding.rvStaff.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStaff.adapter = userAdapter

        actorUser = mainViewModel.uiState.value.currentUser
        actorUser?.let {
            lastLoadedRoleKey = "${it.uid}:${it.roleGlobal}"
            managerViewModel.loadDashboard(it)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    managerViewModel.managedUsers.collect { users ->
                        binding.progressBar.visibility = View.GONE
                        
                        // Resolve Store Names
                        val storeMap = managerViewModel.stores.value.associate { it.storeId to it.name }
                        userAdapter.setStoreMap(storeMap)
                        
                        // Filter: Remove Staff as requested
                        val filteredUsers = users.filter { it.roleGlobal != "staff" }
                        userAdapter.submitList(filteredUsers)
                        
                        binding.tvEmptyState.visibility = if (filteredUsers.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    mainViewModel.uiState.collect { state ->
                        val latestActor = state.currentUser ?: return@collect
                        val roleKey = "${latestActor.uid}:${latestActor.roleGlobal}"
                        if (lastLoadedRoleKey != roleKey) {
                            lastLoadedRoleKey = roleKey
                            actorUser = latestActor
                            managerViewModel.loadDashboard(latestActor)
                        }
                    }
                }
                launch {
                    managerViewModel.stores.collect { stores ->
                        val storeMap = stores.associate { it.storeId to it.name }
                        userAdapter.setStoreMap(storeMap)
                    }
                }
                launch {
                    managerViewModel.managerMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            managerViewModel.resetMessage()
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