package com.chesslock.domain.leaderboard

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardRanksTest {
    private val config = AppConfig.DEFAULT

    private fun entry(id: String, elo: Int, played: Int = 5, nick: String = id) =
        LeaderboardEntry(id, nick, elo, played)

    @Test
    fun emptyBoardHasNoRowsAndNoMyRank() {
        val snapshot = LeaderboardRanks.snapshot(emptyList(), "me", config.leaderboardTopLimit)

        assertTrue(snapshot.top.isEmpty())
        assertNull(snapshot.myRank)
        assertEquals(0, snapshot.totalPlayers)
    }

    @Test
    fun sortsByEloDescendingAndAssignsPositionsFromOne() {
        val snapshot = LeaderboardRanks.snapshot(
            listOf(entry("low", 900), entry("high", 2100), entry("mid", 1500)),
            "high",
            config.leaderboardTopLimit,
        )

        assertEquals(listOf("high", "mid", "low"), snapshot.top.map { it.entry.playerId })
        assertEquals(listOf(1, 2, 3), snapshot.top.map { it.rank })
        assertEquals(1, snapshot.myRank?.rank)
    }

    @Test
    fun equalEloIsBrokenByRatedPuzzlesThenPlayerId() {
        val snapshot = LeaderboardRanks.snapshot(
            listOf(
                entry("z", 1500, played = 3),
                entry("a", 1500, played = 3),
                entry("b", 1500, played = 9),
            ),
            "a",
            config.leaderboardTopLimit,
        )

        assertEquals(listOf("b", "a", "z"), snapshot.top.map { it.entry.playerId })
        assertEquals(2, snapshot.myRank?.rank)
    }

    @Test
    fun topLimitTruncatesOnlyTheVisibleRows() {
        val entries = (1..25).map { entry("p$it", 1000 + it) }
        val snapshot = LeaderboardRanks.snapshot(entries, "p25", config.leaderboardTopLimit)

        assertEquals(config.leaderboardTopLimit, snapshot.top.size)
        assertEquals(25, snapshot.totalPlayers)
        assertEquals(1, snapshot.myRank?.rank)
    }

    @Test
    fun myRankIsResolvedEvenOutsideTheTop() {
        val entries = (1..30).map { entry("p$it", 1000 + it) }
        val snapshot = LeaderboardRanks.snapshot(entries, "p1", config.leaderboardTopLimit)

        assertFalse(snapshot.top.any { it.entry.playerId == "p1" })
        assertEquals(30, snapshot.myRank?.rank)
    }

    @Test
    fun unknownPlayerHasNoRank() {
        val snapshot = LeaderboardRanks.snapshot(listOf(entry("a", 1200)), "missing", 10)

        assertNull(snapshot.myRank)
        assertEquals(1, snapshot.top.size)
    }

    @Test
    fun nonPositiveTopLimitHidesRowsButKeepsTotalAndMyRank() {
        val snapshot = LeaderboardRanks.snapshot(listOf(entry("a", 1200)), "a", -3)

        assertTrue(snapshot.top.isEmpty())
        assertEquals(1, snapshot.totalPlayers)
        assertEquals(1, snapshot.myRank?.rank)
    }

    @Test
    fun nicknameIsTrimmedAndWhitespaceCollapsed() {
        assertEquals("Ada Lovelace", LeaderboardRanks.normalizeNickname("  Ada   Lovelace  ", config))
        assertTrue(LeaderboardRanks.isValidNickname("Žaneta-Kováč", config))
    }

    @Test
    fun invalidNicknameFallsBackToDefault() {
        assertEquals(config.leaderboardDefaultNickname, LeaderboardRanks.normalizeNickname("  ", config))
        assertEquals(config.leaderboardDefaultNickname, LeaderboardRanks.normalizeNickname("x", config))
        assertEquals(
            config.leaderboardDefaultNickname,
            LeaderboardRanks.normalizeNickname("a".repeat(config.leaderboardNicknameMaxLength + 1), config),
        )
        assertFalse(LeaderboardRanks.isValidNickname("<script>", config))
    }

    @Test
    fun defaultNicknameIsStableAndDistinguishablePerPlayer() {
        val first = LeaderboardRanks.defaultNickname("abcdef123456", config)
        val second = LeaderboardRanks.defaultNickname("zzzzzz999999", config)

        assertEquals("Player-456", first)
        assertEquals("Player-999", second)
        assertEquals(first, LeaderboardRanks.defaultNickname("abcdef123456", config))
        assertTrue(LeaderboardRanks.isValidNickname(first, config))
    }
}
