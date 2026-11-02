package com.chesslock.domain.puzzle

/**
 * One Lichess puzzle (CC0). [moves] are UCI moves from [fen]: the FIRST move is the opponent's and is
 * played automatically before the puzzle is shown; then the player and the opponent alternate, and
 * the last move is always the player's.
 */
data class Puzzle(
    val id: String,
    val fen: String,
    val moves: List<String>,
    val rating: Int,
    val ratingDeviation: Int,
    val popularity: Int,
    val nbPlays: Int,
    val themes: List<String>,
    val gameUrl: String,
    val openingTags: List<String>,
) {
    init {
        require(moves.size >= 2 && moves.size % 2 == 0) {
            "Puzzle $id: moves must be opponent move + player/opponent pairs, got ${moves.size}"
        }
    }

    /** Side to move in [fen] makes the first (automatic) move; the player is the other side. */
    val playerIsWhite: Boolean
        get() = fen.split(' ').getOrNull(1) == "b"
}

/**
 * Parser for the Lichess puzzle CSV (database.lichess.org). Columns:
 * PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags
 */
object LichessPuzzleCsv {
    const val HEADER =
        "PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags"

    private val uciMove = Regex("^[a-h][1-8][a-h][1-8][qrbn]?$")

    /** Parses one data line. Throws [IllegalArgumentException] with a readable reason if malformed. */
    fun parseLine(line: String): Puzzle {
        val f = splitCsv(line)
        require(f.size >= 9) { "Expected at least 9 columns, got ${f.size}: $line" }
        val id = f[0].trim()
        require(id.isNotEmpty()) { "Empty puzzle id: $line" }
        val fen = f[1].trim()
        require(fen.split(' ').size == 6) { "Puzzle $id: FEN needs 6 fields: $fen" }
        val moves = f[2].trim().split(' ').filter { it.isNotEmpty() }
        moves.forEach { require(uciMove.matches(it)) { "Puzzle $id: bad UCI move '$it'" } }
        fun int(index: Int, name: String): Int =
            f[index].trim().toIntOrNull() ?: throw IllegalArgumentException("Puzzle $id: bad $name '${f[index]}'")
        return Puzzle(
            id = id,
            fen = fen,
            moves = moves,
            rating = int(3, "Rating"),
            ratingDeviation = int(4, "RatingDeviation"),
            popularity = int(5, "Popularity"),
            nbPlays = int(6, "NbPlays"),
            themes = words(f[7]),
            gameUrl = f[8].trim(),
            openingTags = words(f.getOrElse(9) { "" }),
        )
    }

    /**
     * Parses a whole file. The header line and blank lines are skipped. Malformed lines are skipped
     * and reported through [onError] (so one bad row never loses the pack).
     */
    fun parseAll(lines: Sequence<String>, onError: (lineNumber: Int, message: String) -> Unit = { _, _ -> }): List<Puzzle> {
        val result = mutableListOf<Puzzle>()
        lines.forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("PuzzleId")) return@forEachIndexed
            try {
                result += parseLine(line)
            } catch (e: IllegalArgumentException) {
                onError(index + 1, e.message ?: "invalid line")
            }
        }
        return result
    }

    private fun words(field: String): List<String> = field.trim().split(' ').filter { it.isNotEmpty() }

    /** Minimal CSV split with support for double-quoted fields. */
    private fun splitCsv(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> { cur.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { out += cur.toString(); cur.setLength(0) }
                else -> cur.append(c)
            }
            i++
        }
        out += cur.toString()
        return out
    }
}
