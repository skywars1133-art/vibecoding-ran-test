package com.chesslock.domain.leaderboard

import com.chesslock.domain.config.AppConfig

/** Pure ranking rules. No Android, no Firebase, no I/O — fully unit-testable. */
object LeaderboardRanks {
    /**
     * Nicknames are trimmed, inner whitespace is collapsed and out-of-range or unexpected
     * characters fall back to the configured default. This keeps the same rule on the client and
     * in the server-side validation rules.
     */
    fun normalizeNickname(raw: String, config: AppConfig): String {
        val cleaned = raw.trim().replace(Regex("\\s+"), " ")
        return if (isValidNickname(cleaned, config)) cleaned else config.leaderboardDefaultNickname
    }

    /**
     * Default display name until an editable nickname profile exists: the configured base plus a
     * short, stable suffix from the anonymous id so two installs are distinguishable.
     */
    fun defaultNickname(playerId: String, config: AppConfig): String {
        val suffix = playerId.filter { it.isLetterOrDigit() }.takeLast(3)
        val base = config.leaderboardDefaultNickname
        return normalizeNickname(if (suffix.isEmpty()) base else "$base-$suffix", config)
    }

    fun isValidNickname(raw: String, config: AppConfig): Boolean {
        val cleaned = raw.trim().replace(Regex("\\s+"), " ")
        if (cleaned.length !in config.leaderboardNicknameMinLength..config.leaderboardNicknameMaxLength) {
            return false
        }
        // Letters (including Slovak diacritics), digits, space and a few safe separators.
        return cleaned.all { it.isLetterOrDigit() || it == ' ' || it in "-_." }
    }

    /**
     * Sorts by ELO descending, then by rated puzzles played (more experienced wins a tie), then
     * by the stable player id so the order is deterministic. Ranks are plain positions 1..n.
     * [topLimit] only limits the visible rows; [myPlayerId] is still resolved from the full board.
     */
    fun snapshot(
        entries: Collection<LeaderboardEntry>,
        myPlayerId: String,
        topLimit: Int,
    ): LeaderboardSnapshot {
        val ranked = entries.sortedWith(
            compareByDescending<LeaderboardEntry> { it.elo }
                .thenByDescending { it.ratedPuzzlesPlayed }
                .thenBy { it.playerId },
        ).mapIndexed { index, entry -> RankedEntry(rank = index + 1, entry = entry) }
        return LeaderboardSnapshot(
            top = ranked.take(topLimit.coerceAtLeast(0)),
            myRank = ranked.firstOrNull { it.entry.playerId == myPlayerId },
            totalPlayers = ranked.size,
        )
    }
}
