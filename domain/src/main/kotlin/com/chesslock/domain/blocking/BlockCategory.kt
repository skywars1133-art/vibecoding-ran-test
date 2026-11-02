package com.chesslock.domain.blocking

/** Categories the user can block (SPEC 1). Stored by [storageKey] so names can change safely. */
enum class BlockCategory(val storageKey: String) {
    SOCIAL("social"),
    GAMES("games"),
    VIDEO("video"),
    COMMUNICATION("communication"),
    SHOPPING("shopping"),
    OTHER("other"),
    ;

    companion object {
        fun fromStorageKey(key: String?): BlockCategory? =
            entries.firstOrNull { it.storageKey == key }
    }
}
