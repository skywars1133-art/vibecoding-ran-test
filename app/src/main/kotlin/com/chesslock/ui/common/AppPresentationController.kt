package com.chesslock.ui.common

import android.content.Context
import com.chesslock.data.AppLanguage
import com.chesslock.data.SettingsStore
import com.chesslock.widget.BudgetWidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps in-app language, per-app locale, launcher shortcuts and widget text in sync. */
@Singleton
class AppPresentationController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
) {
    suspend fun syncFromSettings() {
        val language = settingsStore.settings.first().language
        withContext(Dispatchers.Main.immediate) { apply(language) }
    }

    fun apply(language: AppLanguage) {
        AppLocaleController.apply(context, language)
        AppShortcutPublisher.publish(context, language)
        BudgetWidgetUpdater.requestUpdate(context)
    }
}
