package com.example

import android.app.Application
import android.content.Intent
import android.util.Log
import kotlin.system.exitProcess

class PanicLinkApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("PanicLinkApplication", "Application onCreate: Initializing global robustness layers.")

        // Global uncaught exception interceptor
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("PanicLinkApplication", "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            
            try {
                // Keep the app alive by starting MainActivity again
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("PanicLinkApplication", "Failed to restart MainActivity from exception handler", e)
            }

            // Exit cleanly so system restarts sticky background elements
            exitProcess(2)
        }
    }
}
