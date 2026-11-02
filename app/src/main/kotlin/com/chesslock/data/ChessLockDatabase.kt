package com.chesslock.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PuzzleEntity::class], version = 1, exportSchema = true)
abstract class ChessLockDatabase : RoomDatabase() {
    abstract fun puzzleDao(): PuzzleDao
}
