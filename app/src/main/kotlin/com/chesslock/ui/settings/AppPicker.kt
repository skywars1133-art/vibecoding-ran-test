package com.chesslock.ui.settings

import com.chesslock.data.AppBlockOverride

/** One row in the per-app blocking picker, with the override currently applied. */
data class AppPickerEntry(
    val packageName: String,
    val label: String,
    val override: AppBlockOverride,
)

/** Case-insensitive search over app labels and package names. */
internal fun filterApps(apps: List<AppPickerEntry>, query: String): List<AppPickerEntry> {
    val needle = query.trim()
    if (needle.isEmpty()) return apps
    return apps.filter {
        it.label.contains(needle, ignoreCase = true) ||
            it.packageName.contains(needle, ignoreCase = true)
    }
}
