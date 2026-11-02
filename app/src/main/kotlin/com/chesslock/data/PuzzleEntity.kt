package com.chesslock.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chesslock.domain.puzzle.Puzzle

@Entity(tableName = "puzzles")
data class PuzzleEntity(
    @PrimaryKey val id: String,
    val fen: String,
    val moves: String,
    val rating: Int,
    val ratingDeviation: Int,
    val popularity: Int,
    val nbPlays: Int,
    val themes: String,
    val gameUrl: String,
    val openingTags: String,
    val solved: Boolean = false,
)

fun PuzzleEntity.toDomain(): Puzzle = Puzzle(
    id = id,
    fen = fen,
    moves = moves.split(' ').filter { it.isNotBlank() },
    rating = rating,
    ratingDeviation = ratingDeviation,
    popularity = popularity,
    nbPlays = nbPlays,
    themes = themes.split(' ').filter { it.isNotBlank() },
    gameUrl = gameUrl,
    openingTags = openingTags.split(' ').filter { it.isNotBlank() },
)

fun Puzzle.toEntity(): PuzzleEntity = PuzzleEntity(
    id = id,
    fen = fen,
    moves = moves.joinToString(" "),
    rating = rating,
    ratingDeviation = ratingDeviation,
    popularity = popularity,
    nbPlays = nbPlays,
    themes = themes.joinToString(" "),
    gameUrl = gameUrl,
    openingTags = openingTags.joinToString(" "),
)
