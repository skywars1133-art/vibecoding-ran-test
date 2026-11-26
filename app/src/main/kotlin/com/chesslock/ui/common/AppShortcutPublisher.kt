package com.chesslock.ui.common

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.chesslock.MainActivity
import com.chesslock.R
import com.chesslock.ShortcutDestination
import com.chesslock.data.AppLanguage

internal data class AppShortcutSpec(
    val id: String,
    val destination: ShortcutDestination,
    @StringRes val shortLabelRes: Int,
    @StringRes val longLabelRes: Int,
    @DrawableRes val iconRes: Int,
)

/** Three actions in a stable order, so pinned shortcuts keep their identity across language changes. */
internal fun appShortcutSpecs(): List<AppShortcutSpec> = listOf(
    AppShortcutSpec(
        id = "puzzle",
        destination = ShortcutDestination.PUZZLE,
        shortLabelRes = R.string.shortcut_puzzle_short,
        longLabelRes = R.string.shortcut_puzzle_long,
        iconRes = R.drawable.ic_shortcut_puzzle,
    ),
    AppShortcutSpec(
        id = "statistics",
        destination = ShortcutDestination.STATISTICS,
        shortLabelRes = R.string.shortcut_stats_short,
        longLabelRes = R.string.shortcut_stats_long,
        iconRes = R.drawable.ic_shortcut_stats,
    ),
    AppShortcutSpec(
        id = "settings",
        destination = ShortcutDestination.SETTINGS,
        shortLabelRes = R.string.shortcut_settings_short,
        longLabelRes = R.string.shortcut_settings_long,
        iconRes = R.drawable.ic_shortcut_settings,
    ),
)

/**
 * Publishes launcher shortcuts at runtime instead of via `res/xml/shortcuts.xml`, so their labels
 * follow the in-app language and update immediately when it changes.
 */
internal object AppShortcutPublisher {
    fun publish(context: Context, language: AppLanguage) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val localized = context.withAppLanguage(language)
        val shortcuts = appShortcutSpecs().map { spec ->
            val intent = Intent(spec.destination.action)
                .setClass(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ShortcutInfo.Builder(context, spec.id)
                .setShortLabel(localized.getString(spec.shortLabelRes))
                .setLongLabel(localized.getString(spec.longLabelRes))
                .setIcon(Icon.createWithResource(context, spec.iconRes))
                .setIntent(intent)
                .build()
        }
        manager.setDynamicShortcuts(shortcuts)
    }
}
