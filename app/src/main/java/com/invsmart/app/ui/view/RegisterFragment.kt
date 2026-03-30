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
    private val passwordSpecialCharRegex = Regex("[^A-Za-z0-9]")

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
            val validationError = getRegisterInputError(email, password)
            if (validationError == null) {
                hasHandledRegisterResult = false
                viewModel.register(email, password)
            } else {
                Toast.makeText(requireContext(), validationError, Toast.LENGTH_SHORT).show()
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
                                val navController = findNavController()

                                Toast.makeText(requireContext(), "Đăng ký thành công, vui lòng đăng nhập", Toast.LENGTH_SHORT).show()

                                navController.getBackStackEntry(R.id.loginFragment)
                                    .savedStateHandle
                                    .set("registered_email", registeredEmail)
                                navController.getBackStackEntry(R.id.loginFragment)
                                    .savedStateHandle
                                    .set("registered_password", registeredPassword)

                                viewModel.logout()
                                navController.popBackStack()
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

    private fun getRegisterInputError(email: String, password: String): String? {
        if (email.isEmpty()) {
            return "Email không được để trống"
        }
        if (password.length < 8) {
            return "Mật khẩu phải có ít nhất 8 ký tự"
        }
        if (!password.any { it.isDigit() }) {
            return "Mật khẩu phải có ít nhất 1 chữ số"
        }
        if (!password.any { it.isLowerCase() }) {
            return "Mật khẩu phải có ít nhất 1 chữ cái thường"
        }
        if (!password.any { it.isUpperCase() }) {
            return "Mật khẩu phải có ít nhất 1 chữ cái hoa"
        }
        if (!passwordSpecialCharRegex.containsMatchIn(password)) {
            return "Mật khẩu phải có ít nhất 1 ký tự đặc biệt"
        }
        return null
    }
}