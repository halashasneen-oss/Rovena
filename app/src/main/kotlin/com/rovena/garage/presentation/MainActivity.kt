package com.rovena.garage.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.rovena.garage.R
import com.rovena.garage.RovenaApp
import com.rovena.garage.databinding.ActivityMainBinding
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.common.viewModelFactory
import com.rovena.garage.presentation.lock.LockActivity
import com.rovena.garage.presentation.onboarding.OnboardingActivity
import com.rovena.garage.presentation.quickadd.QuickAddSheet
import com.rovena.garage.utils.applyEdgeToEdgeInsets
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val startViewModel: AppStartViewModel by viewModels { viewModelFactory { AppStartViewModel(appContainer) } }
    private val mainViewModel: MainViewModel by viewModels { viewModelFactory { MainViewModel(appContainer) } }

    private val app: RovenaApp get() = application as RovenaApp

    private val lockLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            finish()
        } else {
            app.markAuthenticated()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

        binding.fabQuickAdd.setOnClickListener {
            val vehicleId = mainViewModel.currentVehicle.value?.id
            if (vehicleId != null) {
                QuickAddSheet.newInstance(vehicleId).show(supportFragmentManager, QuickAddSheet.TAG)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val topLevel = setOf(R.id.nav_home, R.id.nav_garage, R.id.nav_service, R.id.nav_insights, R.id.nav_more)
            val visible = destination.id in topLevel
            binding.bottomNav.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
            binding.fabQuickAdd.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                startViewModel.destination.collect { destination ->
                    when (destination) {
                        StartDestination.ONBOARDING -> {
                            startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                            finish()
                        }
                        StartDestination.MAIN -> checkAppLock()
                        null -> Unit
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (startViewModel.destination.value == StartDestination.MAIN) {
            checkAppLock()
        }
    }

    private fun checkAppLock() {
        if (!app.requiresReauth) return
        lifecycleScope.launch {
            val settings = appContainer.settingsRepository.getOrDefault()
            if (settings.appLockEnabled) {
                lockLauncher.launch(Intent(this@MainActivity, LockActivity::class.java))
            } else {
                app.markAuthenticated()
            }
        }
    }
}
