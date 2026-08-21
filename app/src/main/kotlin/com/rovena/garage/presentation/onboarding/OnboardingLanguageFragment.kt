package com.rovena.garage.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rovena.garage.R
import com.rovena.garage.databinding.FragmentOnboardingLanguageBinding
import com.rovena.garage.domain.model.AppLanguage
import com.rovena.garage.presentation.common.appContainer
import kotlinx.coroutines.launch

class OnboardingLanguageFragment : Fragment(R.layout.fragment_onboarding_language) {

    private var _binding: FragmentOnboardingLanguageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOnboardingLanguageBinding.bind(inflater.inflate(R.layout.fragment_onboarding_language, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.langEnglish.isChecked = true
        binding.languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val language = when (checkedId) {
                binding.langArabic.id -> AppLanguage.ARABIC
                binding.langFrench.id -> AppLanguage.FRENCH
                binding.langSpanish.id -> AppLanguage.SPANISH
                else -> AppLanguage.ENGLISH
            }
            lifecycleScope.launch { appContainer.settingsRepository.setLanguage(language) }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
