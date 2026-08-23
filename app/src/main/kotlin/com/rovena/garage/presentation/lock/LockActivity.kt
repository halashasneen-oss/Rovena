package com.rovena.garage.presentation.lock

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rovena.garage.R
import com.rovena.garage.data.repository.PinVerifyResult
import com.rovena.garage.databinding.ActivityLockBinding
import com.rovena.garage.presentation.common.appContainer
import com.rovena.garage.utils.PinHasher
import com.rovena.garage.utils.applyEdgeToEdgeInsets
import kotlinx.coroutines.launch

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private val dots = mutableListOf<ImageView>()
    private var enteredPin = StringBuilder()

    /**
     * The PIN's own digit count, so the keypad knows exactly when a full entry has been
     * typed instead of re-checking the hash after every keystroke (which would both waste
     * the deliberately-slow PBKDF2 cost and, worse, spuriously count each not-yet-complete
     * partial entry as a failed attempt toward the lockout). Null only for a PIN set before
     * this field existed - see the fallback branch in [onDigit].
     */
    private var targetPinLength: Int? = null
    private var lockoutTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyEdgeToEdgeInsets(binding.root)

        setupKeypad()

        lifecycleScope.launch {
            val settings = appContainer.settingsRepository.getOrDefault()
            targetPinLength = settings.pinLength
            setupDots(settings.pinLength ?: PinHasher.MAX_PIN_LENGTH)

            val lockoutUntil = settings.pinLockoutUntilMillis
            if (lockoutUntil != null && System.currentTimeMillis() < lockoutUntil) {
                showLockout(lockoutUntil)
            }

            if (settings.biometricEnabled && canUseBiometric()) {
                binding.keyBiometric.visibility = View.VISIBLE
                binding.keyBiometric.setOnClickListener { showBiometricPrompt() }
                if (lockoutUntil == null || System.currentTimeMillis() >= lockoutUntil) {
                    showBiometricPrompt()
                }
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

    private fun setupDots(count: Int) {
        binding.pinDotsContainer.removeAllViews()
        dots.clear()
        repeat(count) {
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
        if (isLockedOut()) return
        val maxLen = targetPinLength ?: PinHasher.MAX_PIN_LENGTH
        if (enteredPin.length >= maxLen) return
        enteredPin.append(digit)
        refreshDots()

        val knownLength = targetPinLength
        if (knownLength != null) {
            if (enteredPin.length == knownLength) checkPin()
        } else if (enteredPin.length >= PinHasher.MIN_PIN_LENGTH) {
            // Legacy PIN set before pinLength was tracked - fall back to checking on every
            // keystroke once the minimum length is reached, same as before this length
            // tracking existed. checkPin() backfills pinLength on success so this path is
            // only ever taken once per legacy install.
            checkPin()
        }
    }

    private fun isLockedOut(): Boolean = lockoutTimer != null

    private fun checkPin() {
        val attemptLength = enteredPin.length
        lifecycleScope.launch {
            when (val result = appContainer.settingsRepository.verifyPin(enteredPin.toString())) {
                is PinVerifyResult.Success -> {
                    if (targetPinLength == null) appContainer.settingsRepository.recordPinLength(attemptLength)
                    unlock()
                }
                is PinVerifyResult.NoPinSet -> unlock()
                is PinVerifyResult.LockedOut -> showLockout(result.untilMillis)
                is PinVerifyResult.WrongPin -> {
                    // With a known target length this is always a complete, deliberate entry;
                    // with the legacy fallback it may just be a not-yet-complete partial guess
                    // of a longer PIN, so only surface an error once the keypad is fully spent.
                    if (targetPinLength != null || enteredPin.length >= PinHasher.MAX_PIN_LENGTH) {
                        binding.lockError.text = getString(R.string.lock_wrong_pin)
                        binding.lockError.visibility = View.VISIBLE
                    }
                    enteredPin.clear()
                    refreshDots()
                }
            }
        }
    }

    private fun showLockout(untilMillis: Long) {
        lockoutTimer?.cancel()
        enteredPin.clear()
        refreshDots()
        binding.keyBiometric.visibility = View.GONE

        val remaining = (untilMillis - System.currentTimeMillis()).coerceAtLeast(0)
        lockoutTimer = object : CountDownTimer(remaining, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt() + 1
                binding.lockError.text = getString(R.string.lock_too_many_attempts, seconds)
                binding.lockError.visibility = View.VISIBLE
            }

            override fun onFinish() {
                lockoutTimer = null
                binding.lockError.visibility = View.INVISIBLE
            }
        }.also { it.start() }
    }

    private fun unlock() {
        lockoutTimer?.cancel()
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
    }
}
