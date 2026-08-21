package com.rovena.garage.presentation.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rovena.garage.R
import com.rovena.garage.presentation.vehicleform.VehicleFormFragment

class OnboardingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> OnboardingStepFragment.newInstance(R.drawable.ic_car_placeholder, R.string.onboarding_welcome_title, R.string.onboarding_welcome_message)
        1 -> OnboardingStepFragment.newInstance(R.drawable.ic_inspection, R.string.onboarding_privacy_step_title, R.string.onboarding_privacy_step_message)
        2 -> OnboardingLanguageFragment()
        else -> VehicleFormFragment.newInstance(vehicleId = 0L, onboardingMode = true)
    }
}
