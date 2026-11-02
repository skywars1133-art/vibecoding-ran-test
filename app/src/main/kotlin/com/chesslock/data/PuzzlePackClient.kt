package com.chesslock.data

import android.util.Log
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.puzzle.LichessPuzzleCsv
import com.chesslock.domain.puzzle.Puzzle
import com.chesslock.domain.puzzle.PuzzlePackPlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads one rating-band CSV from the fixed `puzzle-packs` GitHub Release (SPEC 2). The file is
 * small (about 150 rows), so it is parsed from the response stream and inserted in one Room
 * transaction instead of being written to disk first.
 */
@Singleton
class PuzzlePackClient @Inject constructor(
    private val config: AppConfig,
) {
    suspend fun fetch(center: Int, version: Long = currentUtcDay()): List<Puzzle> =
        withContext(Dispatchers.IO) {
            val url = PuzzlePackPlanner.packUrl(config.puzzlePackBaseUrl, center, version)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = config.puzzlePackConnectTimeoutMs
                readTimeout = config.puzzlePackReadTimeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/csv,text/plain;q=0.9,*/*;q=0.8")
            }
            try {
                val status = connection.responseCode
                if (status !in 200..299) {
                    throw IOException("Puzzle pack download failed with HTTP $status")
                }
                connection.inputStream.bufferedReader().useLines { lines ->
                    LichessPuzzleCsv.parseAll(lines) { lineNumber, message ->
                        Log.w(TAG, "Skipped pack line $lineNumber: $message")
                    }
                }
            } finally {
                connection.disconnect()
            }
        }

    companion object {
        private const val TAG = "PuzzlePackClient"
        private const val USER_AGENT = "ChessLock-Android"

        /** UTC day used only to bypass a cached release asset; the server ignores the query value. */
        fun currentUtcDay(clock: Clock = Clock.systemUTC()): Long = LocalDate.now(clock).toEpochDay()
    }
}
