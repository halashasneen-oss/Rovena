package com.rovena.garage.presentation.common

import android.os.SystemClock
import android.view.View

private const val DEFAULT_DEBOUNCE_MS = 600L

/**
 * Ignores a repeat tap within [debounceMs] of the previous accepted one, so a double
 * tap on Save/Delete can't fire two DB writes (or two nav pops) before the first
 * completes - a plain setOnClickListener has no such guard anywhere in this app.
 * Uses elapsedRealtime rather than wall-clock time so a clock change mid-tap can't
 * defeat the debounce.
 */
fun View.setOnDebouncedClickListener(debounceMs: Long = DEFAULT_DEBOUNCE_MS, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime >= debounceMs) {
            lastClickTime = now
            action(view)
        }
    }
}
