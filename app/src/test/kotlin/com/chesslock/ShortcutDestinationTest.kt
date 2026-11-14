package com.chesslock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutDestinationTest {

    @Test
    fun `each shortcut action maps to its destination`() {
        ShortcutDestination.entries.forEach { destination ->
            assertEquals(destination, ShortcutDestination.fromAction(destination.action))
        }
    }

    @Test
    fun `unknown action is ignored`() {
        assertNull(ShortcutDestination.fromAction("com.chesslock.shortcut.UNKNOWN"))
    }

    @Test
    fun `missing action is ignored`() {
        assertNull(ShortcutDestination.fromAction(null))
    }

    @Test
    fun `shortcut actions are unique`() {
        val actions = ShortcutDestination.entries.map { it.action }
        assertEquals(actions.size, actions.distinct().size)
    }
}
