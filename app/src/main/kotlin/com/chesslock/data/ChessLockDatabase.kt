package com.chesslock.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PuzzleEntity::class, AttemptEntity::class, UsageDayEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ChessLockDatabase : RoomDatabase() {
    abstract fun puzzleDao(): PuzzleDao
    abstract fun statsDao(): StatsDao
}

/**
 * Adds the local statistics tables. Existing puzzle rows (including solved flags) are untouched,
 * so upgrading keeps the offline stock and current progress.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `attempts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`puzzleId` TEXT NOT NULL, " +
                "`puzzleRating` INTEGER NOT NULL, " +
                "`themes` TEXT NOT NULL, " +
                "`solved` INTEGER NOT NULL, " +
                "`skipped` INTEGER NOT NULL, " +
                "`hintsUsed` INTEGER NOT NULL, " +
                "`eloBefore` INTEGER NOT NULL, " +
                "`eloAfter` INTEGER NOT NULL, " +
                "`rewardSeconds` INTEGER NOT NULL, " +
                "`playedAtEpochMillis` INTEGER NOT NULL, " +
                "`day` TEXT NOT NULL" +
                ")",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attempts_puzzleId` ON `attempts` (`puzzleId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attempts_playedAtEpochMillis` " +
                "ON `attempts` (`playedAtEpochMillis`)",
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `usage_days` (" +
                "`date` TEXT NOT NULL, " +
                "`usedSeconds` INTEGER NOT NULL, " +
                "`earnedSeconds` INTEGER NOT NULL, " +
                "PRIMARY KEY(`date`)" +
                ")",
        )
    }
}
