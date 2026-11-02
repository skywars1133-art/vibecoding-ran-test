package com.chesslock.blocking

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Starts and stops [BlockingService] from the app, settings, onboarding and the boot receiver. */
@Singleton
class BlockingServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _running = MutableStateFlow(false)

    /** True while [BlockingService] is alive; used by the "Protection is not active" status. */
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun start() {
        runCatching {
            ContextCompat.startForegroundService(context, BlockingService.startIntent(context))
        }.onFailure { Log.w(TAG, "Could not start blocking service", it) }
    }

    fun stop() {
        runCatching { context.stopService(Intent(context, BlockingService::class.java)) }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) start() else stop()
    }

    internal fun setRunning(running: Boolean) {
        _running.value = running
    }

    private companion object {
        const val TAG = "BlockingServiceControl"
    }
}
