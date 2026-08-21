package com.rovena.garage.presentation.lock

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.rovena.garage.R
import com.rovena.garage.databinding.ActivityLockBinding
import com.rovena.garage.presentation.common.appContainer
import kotlinx.coroutines.launch

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private val dots = mutableListOf<ImageView>()
    private var enteredPin = StringBuilder()
    private val maxPinLength = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDots()
        setupKeypad()

        lifecycleScope.launch {
            val settings = appContainer.settingsRepository.getOrDefault()
            if (settings.biometricEnabled && canUseBiometric()) {
                binding.keyBiometric.visibility = View.VISIBLE
                binding.keyBiometric.setOnClickListener { showBiometricPrompt() }
                showBiometricPrompt()
            }
        }
    }

    private fun canUseBiometric(): Boolean {
        val manager = BiometricManager.from(this)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlock()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_biometric_prompt_title))
            .setNegativeButtonText(getString(R.string.action_cancel))
            .build()
        prompt.authenticate(info)
    }

    private fun setupDots() {
        repeat(maxPinLength) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.shape_dot_outline)
                layoutParams = android.widget.LinearLayout.LayoutParams(24, 24).apply { marginStart = 8; marginEnd = 8 }
            }
            dots.add(dot)
            binding.pinDotsContainer.addView(dot)
        }
    }

    private fun refreshDots() {
        dots.forEachIndexed { index, dot ->
            dot.setImageResource(if (index < enteredPin.length) R.drawable.shape_dot else R.drawable.shape_dot_outline)
            dot.imageTintList = if (index < enteredPin.length) ColorStateList.valueOf(ContextCompat.getColor(this, R.color.rovena_accent)) else null
        }
    }

    private fun setupKeypad() {
        val keyMap = listOf(
            binding.key0 to "0", binding.key1 to "1", binding.key2 to "2", binding.key3 to "3", binding.key4 to "4",
            binding.key5 to "5", binding.key6 to "6", binding.key7 to "7", binding.key8 to "8", binding.key9 to "9"
        )
        keyMap.forEach { (button, digit) -> button.setOnClickListener { onDigit(digit) } }
        binding.keyBackspace.setOnClickListener {
            if (enteredPin.isNotEmpty()) {
                enteredPin.deleteCharAt(enteredPin.length - 1)
                refreshDots()
                binding.lockError.visibility = View.INVISIBLE
            }
        }
    }

    private fun onDigit(digit: String) {
        if (enteredPin.length >= maxPinLength) return
        enteredPin.append(digit)
        refreshDots()
        if (enteredPin.length >= 4) {
            checkPin()
        }
    }

    private fun checkPin() {
        lifecycleScope.launch {
            val valid = appContainer.settingsRepository.verifyPin(enteredPin.toString())
            when {
                valid -> unlock()
                // Not a match yet - could just be an incomplete longer PIN, so only show an error
                // once the user has typed as many digits as the keypad allows.
                enteredPin.length == maxPinLength -> {
                    binding.lockError.text = getString(R.string.lock_wrong_pin)
                    binding.lockError.visibility = View.VISIBLE
                    enteredPin.clear()
                    refreshDots()
                }
            }
        }
    }

    private fun unlock() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
