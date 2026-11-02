package com.chesslock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {
    /** Row ids of inserted puzzles; ignored duplicates are -1. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(puzzles: List<PuzzleEntity>): List<Long>

    @Query("SELECT * FROM puzzles")
    suspend fun getAll(): List<PuzzleEntity>

    @Query("SELECT COUNT(*) FROM puzzles WHERE solved = 0")
    fun unsolvedCount(): Flow<Int>

    @Query("SELECT id FROM puzzles WHERE solved = 1")
    suspend fun getSolvedIds(): List<String>

    @Query("UPDATE puzzles SET solved = 1 WHERE id = :puzzleId")
    suspend fun markSolved(puzzleId: String)
}
