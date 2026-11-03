package com.chesslock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Query("SELECT * FROM attempts ORDER BY playedAtEpochMillis ASC, id ASC")
    fun observeAttempts(): Flow<List<AttemptEntity>>

    /** Newest loss that has not been completed by a later solve. */
    @Query(
        "SELECT * FROM attempts " +
            "WHERE puzzleId = :puzzleId AND solved = 0 AND skipped = 0 " +
            "ORDER BY id DESC LIMIT 1",
    )
    suspend fun latestOpenLoss(puzzleId: String): AttemptEntity?

    @Query(
        "UPDATE attempts SET solved = 1, rewardSeconds = :rewardSeconds, hintsUsed = :hintsUsed " +
            "WHERE id = :id",
    )
    suspend fun completeLoss(id: Long, rewardSeconds: Long, hintsUsed: Int)

    @Query("SELECT * FROM usage_days WHERE date >= :fromDate ORDER BY date ASC")
    fun observeUsageSince(fromDate: String): Flow<List<UsageDayEntity>>

    @Query("UPDATE usage_days SET usedSeconds = usedSeconds + :seconds WHERE date = :date")
    suspend fun addUsageToExisting(date: String, seconds: Long): Int

    @Query("UPDATE usage_days SET earnedSeconds = earnedSeconds + :seconds WHERE date = :date")
    suspend fun addEarnedToExisting(date: String, seconds: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsageDay(day: UsageDayEntity): Long
}
