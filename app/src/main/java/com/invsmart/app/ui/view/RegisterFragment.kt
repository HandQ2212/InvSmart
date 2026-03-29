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
import com.invsmart.app.data.model.AuthState
import com.invsmart.app.databinding.FragmentRegisterBinding
import com.invsmart.app.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var hasHandledRegisterResult = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            if (email.isNotEmpty() && password.length >= 6) {
                hasHandledRegisterResult = false
                viewModel.register(email, password)
            } else {
                Toast.makeText(requireContext(), "Email không hợp lệ hoặc mật khẩu quá ngắn", Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state.authState) {
                        is AuthState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is AuthState.Authenticated -> {
                            binding.progressBar.visibility = View.GONE
                            if (!hasHandledRegisterResult) {
                                hasHandledRegisterResult = true
                                val registeredEmail = binding.edtEmail.text.toString().trim()
                                val registeredPassword = binding.edtPassword.text.toString().trim()

                                Toast.makeText(requireContext(), "Đăng ký thành công, vui lòng đăng nhập", Toast.LENGTH_SHORT).show()

                                findNavController().previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("registered_email", registeredEmail)
                                findNavController().previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("registered_password", registeredPassword)

                                viewModel.logout()
                                findNavController().popBackStack()
                            }
                        }
                        is AuthState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            if (!hasHandledRegisterResult) {
                                hasHandledRegisterResult = true
                                Toast.makeText(requireContext(), state.authState.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> {
                            binding.progressBar.visibility = View.GONE
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