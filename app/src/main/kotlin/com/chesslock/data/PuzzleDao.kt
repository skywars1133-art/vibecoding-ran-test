package com.chesslock.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PuzzleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(puzzles: List<PuzzleEntity>)

    @Query("SELECT * FROM puzzles")
    suspend fun getAll(): List<PuzzleEntity>

    @Query("SELECT id FROM puzzles WHERE solved = 1")
    suspend fun getSolvedIds(): List<String>

    @Query("UPDATE puzzles SET solved = 1 WHERE id = :puzzleId")
    suspend fun markSolved(puzzleId: String)
}
