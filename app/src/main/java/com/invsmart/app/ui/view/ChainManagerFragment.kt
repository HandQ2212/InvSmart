package com.invsmart.app.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.invsmart.app.databinding.FragmentChainManagerBinding
import dagger.hilt.android.AndroidEntryPoint

import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.invsmart.app.R
import com.invsmart.app.ui.adapter.ChainAdapter
import com.invsmart.app.ui.viewmodel.ChainManagerViewModel
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class ChainManagerFragment : Fragment() {

    private var _binding: FragmentChainManagerBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: com.invsmart.app.ui.MainViewModel by activityViewModels()
    private val viewModel: ChainManagerViewModel by viewModels()
    private lateinit var adapter: ChainAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChainManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ChainAdapter(
            onChainClick = { chain ->
                val action = ChainManagerFragmentDirections.actionChainManagerToUserSelection(
                    chain.chainId,
                    "master",
                    chain.name
                )
                findNavController().navigate(action)
            },
            onDeleteClick = { chain ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Xóa chuỗi")
                    .setMessage("Bạn có chắc chắn muốn xóa chuỗi '${chain.name}' không? Hành động này không thể hoàn tác.")
                    .setPositiveButton("Xóa") { _, _ ->
                        viewModel.deleteChain(chain.chainId)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        binding.rvChains.apply {
            adapter = this@ChainManagerFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Quản lý Cửa hàng"
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Nút Migration (Chỉ hiện cho Admin)
        binding.btnMigrate.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cập nhật dữ liệu cũ")
                .setMessage("Hành động này sẽ cập nhật toàn bộ Product/Order cũ sang cấu trúc Đa chuỗi. Bạn có chắc chắn muốn thực hiện?")
                .setPositiveButton("Cập nhật") { _, _ ->
                    (requireActivity() as? com.invsmart.app.MainActivity)?.let {
                        // Gọi runMigration từ Activity's ViewModel
                        // (Ở đây dùng activityViewModels() nên mainViewModel chính là cái chúng ta cần)
                        mainViewModel.runMigration()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun setupListeners() {
        binding.fabAddChain.setOnClickListener {
            showAddChainDialog()
        }
    }

    private fun showAddChainDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_chain, null)
        val edtChainName = dialogView.findViewById<TextInputEditText>(R.id.edtChainName)
        val edtMasterEmail = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilMasterEmail)
        edtMasterEmail.visibility = View.GONE // Hide email field, we will select from list

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tạo Cửa hàng mới")
            .setView(dialogView)
            .setPositiveButton("Tiếp tục") { _, _ ->
                val name = edtChainName.text.toString()
                if (name.isNotEmpty()) {
                    val bundle = Bundle().apply {
                        putString("storeName", name)
                        putString("role", "master")
                        putString("chainId", "") // Will be generated in Selection or later
                    }
                    findNavController()
                        .navigate(R.id.action_chainManager_to_userSelection, bundle)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.chains.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.message.collect { msg ->
                        if (msg != null) {
                            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.clearMessage()
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
