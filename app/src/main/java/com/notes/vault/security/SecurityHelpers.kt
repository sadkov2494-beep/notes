package com.notes.vault.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val handler = Handler(Looper.getMainLooper())
    private var clearRunnable: Runnable? = null

    fun copyPassword(text: String, label: String = "password") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        clearRunnable?.let { handler.removeCallbacks(it) }
        clearRunnable = Runnable {
            if (clipboard.hasPrimaryClip()) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
        handler.postDelayed(clearRunnable!!, 30_000L)
    }

    fun clearPending() {
        clearRunnable?.let { handler.removeCallbacks(it) }
        clearRunnable = null
    }
}

@Singleton
class PasswordGenerator @Inject constructor() {
    fun generate(
        length: Int = 16,
        uppercase: Boolean = true,
        digits: Boolean = true,
        special: Boolean = true
    ): String {
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val nums = "0123456789"
        val spec = "!@#$%^&*()-_=+[]{}|;:,.<>?"
        var pool = lower
        val required = mutableListOf<Char>()
        if (uppercase) {
            pool += upper
            required.add(upper.random())
        }
        if (digits) {
            pool += nums
            required.add(nums.random())
        }
        if (special) {
            pool += spec
            required.add(spec.random())
        }
        val result = StringBuilder()
        required.forEach { result.append(it) }
        while (result.length < length) {
            result.append(pool.random())
        }
        return result.toString().toList().shuffled().joinToString("")
    }
}
