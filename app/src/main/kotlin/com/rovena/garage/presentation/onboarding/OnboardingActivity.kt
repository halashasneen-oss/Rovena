package com.rovena.garage.presentation.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.rovena.garage.R
import com.rovena.garage.databinding.ActivityOnboardingBinding
import com.rovena.garage.presentation.MainActivity
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.presentation.vehicleform.VehicleFormFragment
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingPagerAdapter
    private val dots = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = OnboardingPagerAdapter(this)
        binding.onboardingPager.adapter = adapter
        binding.onboardingPager.isUserInputEnabled = false

        setupDots()

        binding.onboardingPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateControls(position)
            }
        })

        binding.nextButton.setOnClickListener {
            val current = binding.onboardingPager.currentItem
            if (current < adapter.itemCount - 1) {
                binding.onboardingPager.currentItem = current + 1
            }
        }
        binding.backButton.setOnClickListener {
            val current = binding.onboardingPager.currentItem
            if (current > 0) binding.onboardingPager.currentItem = current - 1
        }

        supportFragmentManager.setFragmentResultListener(VehicleFormFragment.RESULT_KEY, this) { _, _ ->
            completeOnboarding()
        }

        updateControls(0)
    }

    private fun setupDots() {
        repeat(adapter.itemCount) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.shape_dot)
                layoutParams = android.widget.LinearLayout.LayoutParams(20, 20).apply { marginEnd = 12; marginStart = 12 }
                alpha = 0.3f
            }
            dots.add(dot)
            binding.dotsContainer.addView(dot)
        }
        dots.firstOrNull()?.alpha = 1f
    }

    private fun updateControls(position: Int) {
        dots.forEachIndexed { index, dot -> dot.alpha = if (index == position) 1f else 0.3f }
        binding.backButton.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
        val isLastPage = position == adapter.itemCount - 1
        binding.nextButton.visibility = if (isLastPage) View.GONE else View.VISIBLE
        binding.backButton.visibility = if (isLastPage) View.GONE else binding.backButton.visibility
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            appContainer.settingsRepository.setOnboardingCompleted(true)
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            finish()
        }
    }
}
