package com.chesslock

/**
 * Launcher shortcut targets. The action strings must stay in sync with `res/xml/shortcuts.xml`;
 * [fromAction] is unit-tested so an unknown or missing action never opens a random screen.
 */
enum class ShortcutDestination(val action: String) {
    PUZZLE("com.chesslock.shortcut.PUZZLE"),
    STATISTICS("com.chesslock.shortcut.STATISTICS"),
    SETTINGS("com.chesslock.shortcut.SETTINGS"),
    ;

    companion object {
        fun fromAction(action: String?): ShortcutDestination? =
            entries.firstOrNull { it.action == action }
    }
}
