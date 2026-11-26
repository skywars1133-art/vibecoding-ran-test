package com.chesslock.ui.common

import com.chesslock.ShortcutDestination
import com.chesslock.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Guards the dynamic shortcut identities and the localized labels used outside Compose. */
class LauncherLabelTest {

    @Test
    fun languageTagsMatchPlatformLocaleFormat() {
        assertNull(AppLanguage.SYSTEM.languageTag)
        assertEquals("en", AppLanguage.ENGLISH.languageTag)
        assertEquals("sk", AppLanguage.SLOVAK.languageTag)
    }

    @Test
    fun shortcutSpecsHaveStableIdsAndKnownDestinations() {
        val specs = appShortcutSpecs()
        assertEquals(listOf("puzzle", "statistics", "settings"), specs.map { it.id })
        assertEquals(ShortcutDestination.entries.toSet(), specs.map { it.destination }.toSet())
        specs.forEach { spec ->
            assertEquals(spec.destination, ShortcutDestination.fromAction(spec.destination.action))
        }
    }

    @Test
    fun enAndSkProvideLocalizedShortcutsAndNeutralWidgetLabels() {
        val english = readStrings("src/main/res/values/strings.xml")
        val slovak = readStrings("src/main/res/values-sk/strings.xml")
        val launcherNames = listOf(
            "shortcut_puzzle_short",
            "shortcut_puzzle_long",
            "shortcut_stats_short",
            "shortcut_stats_long",
            "shortcut_settings_short",
            "shortcut_settings_long",
            "widget_name",
            "widget_description",
        )
        launcherNames.forEach { name ->
            val en = english[name]
            val sk = slovak[name]
            assertNotNull("EN missing $name", en)
            assertNotNull("SK missing $name", sk)
            assertTrue("EN $name must not be empty", en!!.isNotEmpty())
            assertTrue("SK $name must not be empty", sk!!.isNotEmpty())
        }
        // "Puzzle" is the same word in both languages; the rest must visibly change.
        listOf(
            "shortcut_puzzle_long",
            "shortcut_stats_short",
            "shortcut_stats_long",
            "shortcut_settings_short",
            "shortcut_settings_long",
        ).forEach { name ->
            assertNotEquals("${name} must be localized", english[name], slovak[name])
        }
        // The launcher resolves widget metadata with the system locale, so brand-neutral text
        // prevents a Slovak label from appearing while the app itself is English (or vice versa).
        mapOf(
            "widget_name" to "ChessLock",
            "widget_description" to "ChessLock time budget",
        ).forEach { (name, expected) ->
            assertEquals("${name} must stay neutral in EN", expected, english[name])
            assertEquals("${name} must stay neutral in SK", expected, slovak[name])
        }
    }

    private fun readStrings(path: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }
}
