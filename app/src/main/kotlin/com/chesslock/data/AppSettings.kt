package com.chesslock.data

/** Piece sets the board can render. Text styles need no assets; [CBURNETT] uses bundled artwork. */
enum class PieceStyle(val storageKey: String) {
    CLASSIC("classic"),
    OUTLINE("outline"),
    LETTERS("letters"),
    CBURNETT("cburnett"),
    ;

    companion object {
        val DEFAULT = CLASSIC

        fun fromStorageKey(key: String?): PieceStyle =
            entries.firstOrNull { it.storageKey == key } ?: DEFAULT
    }
}

data class AppSettings(
    val pieceStyle: PieceStyle = PieceStyle.DEFAULT,
)
