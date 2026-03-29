package com.invsmart.app.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.invsmart.app.databinding.FragmentStaffAccountBinding
import com.invsmart.app.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StaffAccountFragment : Fragment() {

    private var _binding: FragmentStaffAccountBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val user = mainViewModel.uiState.value.currentUser
        binding.edtEmail.setText(user?.email.orEmpty())
        binding.edtEmail.isEnabled = false
        binding.edtFullName.setText(user?.fullName.orEmpty())
        binding.edtPhone.setText(user?.phoneNumber.orEmpty())

        binding.btnSave.setOnClickListener {
            mainViewModel.updateProfile(
                fullName = binding.edtFullName.text?.toString().orEmpty(),
                phoneNumber = binding.edtPhone.text?.toString().orEmpty()
            )
            Toast.makeText(requireContext(), "Đã lưu thông tin", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
