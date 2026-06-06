package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

object HapticHelper {
    fun performHapticFeedback(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Distinct confirmation pattern: 3 quick pulses
                    val timings = longArrayOf(0, 100, 80, 100, 80, 200)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 100, 80, 100, 80, 200), -1)
                }
                Log.d("HapticHelper", "Successfully performed haptic feedback confirmation.")
            } else {
                Log.w("HapticHelper", "No vibrator found on device.")
            }
        } catch (e: Exception) {
            Log.e("HapticHelper", "Failed to vibrate", e)
        }
    }
}
