package com.chesslock.placement

import com.chesslock.ui.placement.placementResultOnDone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the retake routing and the wording that tells the user the result will be saved. The
 * callbacks are pure so they can be checked without launching a Compose or DataStore test.
 */
class PlacementRetakeSaveTest {

    @Test
    fun retakeRunsExitCallbackAfterSave() {
        var exits = 0
        placementResultOnDone(retake = true, exit = { exits++ })()
        assertEquals(1, exits)
    }

    @Test
    fun onboardingDoesNotUseExitCallback() {
        var exits = 0
        placementResultOnDone(retake = false, exit = { exits++ })()
        assertEquals(0, exits)
    }

    @Test
    fun retakeWithoutExitCallbackIsSafe() {
        placementResultOnDone(retake = true, exit = null)()
    }

    @Test
    fun bothLocalesDescribeTheRetakeAsSaving() {
        listOf(
            LocaleStrings(
                path = "src/main/res/values/strings.xml",
                saveWords = listOf("save", "saves"),
                unchangedWord = "unchanged",
            ),
            LocaleStrings(
                path = "src/main/res/values-sk/strings.xml",
                saveWords = listOf("ulož", "uložiť"),
                unchangedWord = "nezmení",
            ),
        ).forEach { locale ->
            val values = readStrings(locale.path)
            val saveButton = values["placement_retake_save"]
            assertNotNull("${locale.path}: missing save button", saveButton)
            assertTrue(
                "${locale.path}: save button must mention saving",
                locale.matchesSave(saveButton!!),
            )
            assertFalse(
                "${locale.path}: old practice resource names must be gone",
                values.keys.any { it.contains("placement_practice") },
            )
            listOf(
                "settings_placement_retake_description",
                "placement_retake_notice",
                "placement_retake_result_body",
                "placement_retake_result_early",
            ).forEach { name ->
                val value = values[name]
                assertNotNull("${locale.path}: missing $name", value)
                assertTrue(
                    "${locale.path}: $name must mention saving",
                    locale.matchesSave(value!!),
                )
                assertFalse(
                    "${locale.path}: $name must not claim the ELO is unchanged",
                    value.contains(locale.unchangedWord, ignoreCase = true),
                )
            }
            assertTrue(
                "${locale.path}: result body must show the final estimate",
                values["placement_retake_result_body"]!!.contains("%1\$d"),
            )
        }
    }

    private data class LocaleStrings(
        val path: String,
        val saveWords: List<String>,
        val unchangedWord: String,
    ) {
        fun matchesSave(value: String): Boolean =
            saveWords.any { value.contains(it, ignoreCase = true) }
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
