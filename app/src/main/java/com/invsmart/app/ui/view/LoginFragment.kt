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
import com.invsmart.app.databinding.FragmentLoginBinding
import com.invsmart.app.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private var hasNavigatedAfterAuth = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loginSavedStateHandle = findNavController()
            .getBackStackEntry(R.id.loginFragment)
            .savedStateHandle

        loginSavedStateHandle.getLiveData<String>("registered_email").observe(viewLifecycleOwner) { registeredEmail ->
            if (!registeredEmail.isNullOrEmpty()) {
                binding.edtEmail.setText(registeredEmail)
                loginSavedStateHandle.remove<String>("registered_email")
            }
        }

        loginSavedStateHandle.getLiveData<String>("registered_password").observe(viewLifecycleOwner) { registeredPassword ->
            if (!registeredPassword.isNullOrEmpty()) {
                binding.edtPassword.setText(registeredPassword)
                loginSavedStateHandle.remove<String>("registered_password")
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                hasNavigatedAfterAuth = false
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
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
                            state.currentUser?.let { user ->
                                if (hasNavigatedAfterAuth) {
                                    return@collect
                                }

                                val navController = findNavController()
                                if (navController.currentDestination?.id != R.id.loginFragment) {
                                    return@collect
                                }

                                hasNavigatedAfterAuth = true
                                val effectiveRole = user.roleGlobal.ifBlank { user.role }
                                if (effectiveRole == "master" || effectiveRole == "manager" || user.isMaster) {
                                    navController.navigate(R.id.action_loginFragment_to_nav_manager)
                                } else {
                                    navController.navigate(R.id.action_loginFragment_to_nav_staff)
                                }
                            }
                        }
                        is AuthState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), state.authState.message, Toast.LENGTH_SHORT).show()
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
        hasNavigatedAfterAuth = false
        _binding = null
    }
}