package com.invsmart.app

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.invsmart.app.data.local.SessionManager
import com.invsmart.app.data.model.AuthState
import com.invsmart.app.data.model.User
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
                        is AuthState.Authenticated -> {
                            val role = state.currentUser?.roleGlobal ?: sessionManager.getRole()
                            val startDest = resolveStartDestination(state.currentUser)
                            
                            Log.d("NAV", "Authenticated: role=$role, startDest=$startDest, currentParent=${navController.currentDestination?.parent?.id}")

                            // Buộc phải setGraph nếu vùng hiện tại không khớp
                            if (navController.currentDestination?.parent?.id != startDest) {
                                val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
                                navGraph.setStartDestination(startDest)
                                navController.setGraph(navGraph, null)
                            }
                        }
                        is AuthState.Unauthenticated -> {
                            if (navController.currentDestination?.parent?.id != R.id.nav_auth) {
                                val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
                                navGraph.setStartDestination(R.id.nav_auth)
                                navController.setGraph(navGraph, null)
                            }
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
            navGraph.setStartDestination(resolveStartDestination(null))
            navController.setGraph(navGraph, null)
        }
    }

    private fun resolveStartDestination(user: User?): Int {
        val role = user?.roleGlobal?.lowercase() ?: sessionManager.getRole().lowercase()
        Log.d("NAV", "Resolving destination for role: $role")
        
        return when (role) {
            "admin" -> R.id.nav_admin
            "master" -> R.id.nav_master
            "manager" -> R.id.nav_manager
            "staff" -> R.id.nav_staff
            "unassigned" -> R.id.nav_unassigned
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