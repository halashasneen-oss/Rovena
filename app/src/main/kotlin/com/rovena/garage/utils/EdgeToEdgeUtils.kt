package com.rovena.garage.utils

import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Minimal edge-to-edge migration (spec: Android 15/16 UI safety). targetSdk 35+
 * forces edge-to-edge regardless of what the app requests, so every Activity's
 * root view must consume the system-bar/IME insets as padding itself or content
 * renders behind the status bar, navigation bar, or on-screen keyboard.
 *
 * This pads the whole Activity root once rather than touching every individual
 * screen's layout - the bounded-risk fix that covers every fragment hosted
 * inside without a full per-screen inset audit. The bottom padding tracks
 * whichever is taller of the system nav bar or the IME, so a text field near
 * the bottom of a form is pushed clear of the keyboard the same way
 * `adjustResize` used to before edge-to-edge made that flag insufficient on
 * its own.
 */
fun ComponentActivity.applyEdgeToEdgeInsets(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val initialLeft = root.paddingLeft
    val initialTop = root.paddingTop
    val initialRight = root.paddingRight
    val initialBottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(
            initialLeft + bars.left,
            initialTop + bars.top,
            initialRight + bars.right,
            initialBottom + maxOf(bars.bottom, ime.bottom)
        )
        insets
    }
}
