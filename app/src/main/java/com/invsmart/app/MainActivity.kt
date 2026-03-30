package com.invsmart.app

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.invsmart.app.data.local.SessionManager
import com.invsmart.app.data.model.AuthState
import com.invsmart.app.databinding.ActivityMainBinding
import com.invsmart.app.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels()
    @Inject
    lateinit var sessionManager: SessionManager
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpNavigation(savedInstanceState)
        observeAuthentication()
    }

    private fun observeAuthentication() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state.authState) {
                        is AuthState.Unauthenticated -> {
                            val canNavigateToAuth = navController.currentDestination?.id != R.id.loginFragment &&
                                navController.graph.findNode(R.id.action_global_logout) != null
                            if (canNavigateToAuth) {
                                navController.navigate(R.id.action_global_logout)
                            }
                        }
                        is AuthState.Authenticated -> {
                            // Neu mo app truc tiep vao nav_manager/nav_staff thi khong can dieu huong lai.
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setUpNavigation(savedInstanceState: Bundle?) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        if (savedInstanceState == null) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            navGraph.setStartDestination(resolveStartDestination())
            navController.setGraph(navGraph, null)
        }
    }

    private fun resolveStartDestination(): Int {
        if (!sessionManager.isLoggedIn()) {
            return R.id.nav_auth
        }

        return when (sessionManager.getRole().lowercase()) {
            "master", "manager" -> R.id.nav_manager
            "staff" -> R.id.nav_staff
            else -> R.id.nav_auth
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}