package com.rovena.garage.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentOnboardingStepBinding

/** Simple static icon+title+message step (Welcome, Privacy explanation). */
class OnboardingStepFragment : Fragment(R.layout.fragment_onboarding_step) {

    private var _binding: FragmentOnboardingStepBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingStepBinding.bind(inflater.inflate(R.layout.fragment_onboarding_step, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val iconRes = arguments?.getInt(ARG_ICON) ?: R.drawable.ic_car_placeholder
        val titleRes = arguments?.getInt(ARG_TITLE) ?: R.string.onboarding_welcome_title
        val messageRes = arguments?.getInt(ARG_MESSAGE) ?: R.string.onboarding_welcome_message
        binding.stepIcon.setImageResource(iconRes)
        binding.stepTitle.setText(titleRes)
        binding.stepMessage.setText(messageRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ICON = "icon"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"

        fun newInstance(iconRes: Int, titleRes: Int, messageRes: Int) = OnboardingStepFragment().apply {
            arguments = bundleOf(ARG_ICON to iconRes, ARG_TITLE to titleRes, ARG_MESSAGE to messageRes)
        }
    }
}
