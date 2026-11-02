package com.chesslock.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chesslock.data.SettingsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Restarts blocking after a reboot or an app update (SPEC 1). */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var controller: BlockingServiceController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (settingsStore.settings.first().blockingEnabled) controller.start()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
