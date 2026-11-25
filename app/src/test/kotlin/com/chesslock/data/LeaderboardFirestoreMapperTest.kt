package com.chesslock.data

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.leaderboard.LeaderboardEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LeaderboardFirestoreMapperTest {
    private val config = AppConfig.DEFAULT

    @Test
    fun readsFirestoreShapedDocument() {
        val entry = LeaderboardFirestoreMapper.toEntry(
            "uid-1",
            mapOf(
                LeaderboardFirestoreMapper.FIELD_NICKNAME to "Ada",
                LeaderboardFirestoreMapper.FIELD_ELO to 1942L,
                LeaderboardFirestoreMapper.FIELD_RATED to 37L,
            ),
            config,
        )

        assertEquals(LeaderboardEntry("uid-1", "Ada", 1942, 37), entry)
    }

    @Test
    fun ignoresMalformedDocuments() {
        val base = mapOf<String, Any?>(
            LeaderboardFirestoreMapper.FIELD_NICKNAME to "Ada",
            LeaderboardFirestoreMapper.FIELD_ELO to 1942L,
            LeaderboardFirestoreMapper.FIELD_RATED to 37L,
        )

        assertNull(LeaderboardFirestoreMapper.toEntry("x", emptyMap(), config))
        assertNull(LeaderboardFirestoreMapper.toEntry("x", base + (LeaderboardFirestoreMapper.FIELD_ELO to "1942"), config))
        assertNull(LeaderboardFirestoreMapper.toEntry("x", base + (LeaderboardFirestoreMapper.FIELD_NICKNAME to "x"), config))
        assertNull(LeaderboardFirestoreMapper.toEntry("x", base + (LeaderboardFirestoreMapper.FIELD_ELO to -1L), config))
        assertNull(LeaderboardFirestoreMapper.toEntry("x", base + (LeaderboardFirestoreMapper.FIELD_RATED to -1L), config))
    }

    @Test
    fun writesOnlyTheThreeAllowedChessFields() {
        val document = LeaderboardFirestoreMapper.toDocument(
            LeaderboardEntry("uid-1", "  Ada   Lovelace ", 9999, -2),
            config,
        )

        assertEquals(
            setOf(
                LeaderboardFirestoreMapper.FIELD_NICKNAME,
                LeaderboardFirestoreMapper.FIELD_ELO,
                LeaderboardFirestoreMapper.FIELD_RATED,
            ),
            document.keys,
        )
        assertEquals("Ada Lovelace", document[LeaderboardFirestoreMapper.FIELD_NICKNAME])
        assertEquals(config.eloCeiling, document[LeaderboardFirestoreMapper.FIELD_ELO])
        assertEquals(0, document[LeaderboardFirestoreMapper.FIELD_RATED])
    }
}
