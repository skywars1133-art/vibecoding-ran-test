package com.chesslock.data

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.leaderboard.LeaderboardEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLeaderboardRepositoryTest {
    private val config = AppConfig.DEFAULT
    private val repository = LocalLeaderboardRepository(config)

    @Test
    fun sampleRowsAreVisibleBeforeAnythingIsPublished() = runBlocking {
        val snapshot = repository.load(LocalLeaderboardRepository.LOCAL_PLAYER_ID)

        assertEquals(3, snapshot.totalPlayers)
        assertEquals(3, snapshot.top.size)
        assertEquals(null, snapshot.myRank)
    }

    @Test
    fun publishedEntryIsRankedWithTheSamples() = runBlocking {
        repository.publish(LeaderboardEntry(LocalLeaderboardRepository.LOCAL_PLAYER_ID, "You", 1950, 20))

        val snapshot = repository.load(LocalLeaderboardRepository.LOCAL_PLAYER_ID)
        assertEquals(4, snapshot.totalPlayers)
        assertNotNull(snapshot.myRank)
        assertEquals(3, snapshot.myRank?.rank)
        assertEquals("You", snapshot.myRank?.entry?.nickname)
    }

    @Test
    fun publishingAgainReplacesTheEntryInsteadOfAddingAnother() = runBlocking {
        repository.publish(LeaderboardEntry(LocalLeaderboardRepository.LOCAL_PLAYER_ID, "You", 1000, 1))
        repository.publish(LeaderboardEntry(LocalLeaderboardRepository.LOCAL_PLAYER_ID, "You", 2200, 30))

        val snapshot = repository.load(LocalLeaderboardRepository.LOCAL_PLAYER_ID)
        assertEquals(4, snapshot.totalPlayers)
        assertEquals(1, snapshot.myRank?.rank)
    }

    @Test
    fun publishedStatsAreSanitisedBeforeStorage() = runBlocking {
        repository.publish(LeaderboardEntry(LocalLeaderboardRepository.LOCAL_PLAYER_ID, "  ", -5, -3))

        val snapshot = repository.load(LocalLeaderboardRepository.LOCAL_PLAYER_ID)
        val mine = snapshot.myRank?.entry
        assertEquals(config.leaderboardDefaultNickname, mine?.nickname)
        assertEquals(config.eloFloor, mine?.elo)
        assertEquals(0, mine?.ratedPuzzlesPlayed)
        assertTrue(snapshot.top.isNotEmpty())
    }

    @Test
    fun localIdentityUsesTheDefaultNickname() = runBlocking {
        val identity = LocalLeaderboardIdentity(config)

        assertEquals(LocalLeaderboardRepository.LOCAL_PLAYER_ID, identity.playerId())
        assertEquals(config.leaderboardDefaultNickname, identity.nickname())
    }
}
