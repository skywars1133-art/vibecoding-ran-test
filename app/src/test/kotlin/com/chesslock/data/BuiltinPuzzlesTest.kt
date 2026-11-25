package com.chesslock.data

import com.chesslock.chess.ChessPosition
import com.chesslock.domain.puzzle.LichessPuzzleCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the committed offline baseline: real Lichess rows, valid lines and fully legal solutions. */
class BuiltinPuzzlesTest {

    private val asset = File("src/main/assets/builtin_puzzles.csv")

    @Test
    fun `built-in asset has 300 unique legal puzzles spread across the rating bands`() {
        assertTrue("Missing $asset", asset.exists())
        val errors = mutableListOf<String>()
        val puzzles = LichessPuzzleCsv.parseAll(asset.readLines().asSequence()) { line, message ->
            errors += "line $line: $message"
        }
        assertTrue("CSV errors: ${errors.joinToString("; ")}", errors.isEmpty())
        assertEquals(300, puzzles.size)
        assertEquals(300, puzzles.map { it.id }.toSet().size)

        val buckets = puzzles.groupBy { (it.rating - 600) / 200 }
        assertEquals(10, buckets.size)
        (0..9).forEach { bucket ->
            assertEquals(
                "rating band ${600 + bucket * 200}-${799 + bucket * 200}",
                30,
                buckets[bucket]?.size ?: 0,
            )
        }

        puzzles.forEach { puzzle ->
            val position = ChessPosition(puzzle.fen)
            puzzle.moves.forEach { uci ->
                assertTrue(
                    "Puzzle ${puzzle.id}: illegal move $uci from ${puzzle.fen}",
                    position.play(uci),
                )
            }
        }
    }
}
