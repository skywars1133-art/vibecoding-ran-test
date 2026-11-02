package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class PuzzleSelectorTest {
    private val config = AppConfig.DEFAULT

    private fun puzzle(id: String, rating: Int) = Puzzle(
        id = id, fen = "8/8/8/8/8/8/8/8 w - - 0 1", moves = listOf("a1a2", "b1b2"),
        rating = rating, ratingDeviation = 50, popularity = 90, nbPlays = 1000,
        themes = emptyList(), gameUrl = "", openingTags = emptyList(),
    )

    private val stock = (4..28).map { puzzle("P$it", it * 100) } // ratings 400..2800 step 100

    @Test
    fun returnsNullWhenEverythingIsSolved() {
        val solved = stock.map { it.id }.toSet()
        assertNull(PuzzleSelector.pick(stock, 1500, solved, Random(1), config))
    }

    @Test
    fun returnsNullForEmptyStock() {
        assertNull(PuzzleSelector.pick(emptyList(), 1500, emptySet(), Random(1), config))
    }

    @Test
    fun neverRepeatsSolvedPuzzles() {
        val solved = stock.filter { it.rating in 1300..1700 }.map { it.id }.toSet()
        repeat(200) { seed ->
            val p = PuzzleSelector.pick(stock, 1500, solved, Random(seed.toLong()), config)
            assertNotNull(p)
            assertTrue(p!!.id !in solved)
        }
    }

    @Test
    fun staysCloseToThePlayersLevel() {
        repeat(500) { seed ->
            val p = PuzzleSelector.pick(stock, 1500, emptySet(), Random(seed.toLong()), config)!!
            // sigma 100 offset (rarely beyond ~3 sigma) plus a pool of 3 neighbours
            assertTrue("rating=${p.rating}", p.rating in 1000..2000)
        }
    }

    @Test
    fun showsMildVarietyNotAlwaysTheSamePuzzle() {
        val picks = (0 until 300).map {
            PuzzleSelector.pick(stock, 1500, emptySet(), Random(it.toLong()), config)!!.id
        }.toSet()
        assertTrue("distinct=${picks.size}", picks.size >= 3)
    }

    @Test
    fun sameSeedGivesSamePuzzle() {
        val a = PuzzleSelector.pick(stock, 1500, emptySet(), Random(42), config)
        val b = PuzzleSelector.pick(stock, 1500, emptySet(), Random(42), config)
        assertEquals(a, b)
    }

    @Test
    fun fallsBackToTheClosestAvailableWhenNothingIsNear() {
        val farOnly = listOf(puzzle("X1", 2600), puzzle("X2", 2700))
        val p = PuzzleSelector.pick(farOnly, 800, emptySet(), Random(3), config)
        assertNotNull(p)
    }

    @Test
    fun poolSizeOneAlwaysPicksTheClosest() {
        val exact = config.copy(selectionPoolSize = 1, selectionSigma = 0)
        val p = PuzzleSelector.pick(stock, 1520, emptySet(), Random(7), exact)!!
        assertEquals(1500, p.rating)
    }
}
