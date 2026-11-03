package com.chesslock.ui.settings

import com.chesslock.data.AppBlockOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPickerTest {

    private val entries = listOf(
        AppPickerEntry("com.instagram.android", "Instagram", AppBlockOverride.DEFAULT),
        AppPickerEntry("com.google.android.youtube", "YouTube", AppBlockOverride.ALWAYS_BLOCK),
        AppPickerEntry("com.example.calm", "Calm", AppBlockOverride.NEVER_BLOCK),
    )

    @Test
    fun `blank query keeps every app`() {
        assertEquals(entries, filterApps(entries, ""))
        assertEquals(entries, filterApps(entries, "   "))
    }

    @Test
    fun `search matches labels case-insensitively`() {
        assertEquals(
            listOf(entries[1]),
            filterApps(entries, "youtube"),
        )
    }

    @Test
    fun `search matches package names`() {
        assertEquals(
            listOf(entries[0]),
            filterApps(entries, "instagram.android"),
        )
    }

    @Test
    fun `search trims surrounding whitespace`() {
        assertEquals(listOf(entries[2]), filterApps(entries, "  CALM "))
    }

    @Test
    fun `search without a match returns an empty list`() {
        assertTrue(filterApps(entries, "tiktok").isEmpty())
    }
}
