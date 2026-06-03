package com.invsmart.app.ui.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.invsmart.app.R
import com.invsmart.app.data.model.AuthState
import com.invsmart.app.databinding.FragmentLoginBinding
import com.invsmart.app.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (_binding == null) return@registerForActivityResult

        if (result.resultCode != Activity.RESULT_OK) {
            binding.progressBar.visibility = View.GONE
            return@registerForActivityResult
        }

        handleGoogleSignInResult(result.data)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGoogleSignIn()

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
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoogleLogin.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
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

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken.isNullOrEmpty()) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Không lấy được token Google", Toast.LENGTH_SHORT).show()
                return
            }

            viewModel.loginWithGoogle(idToken)
        } catch (_: ApiException) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
