package com.chesslock.domain.blocking

/** User choices plus per-app overrides (SPEC 1). Overrides are package names, not labels. */
data class BlockRules(
    val blockedCategories: Set<BlockCategory> = emptySet(),
    val alwaysBlockedPackages: Set<String> = emptySet(),
    val neverBlockedPackages: Set<String> = emptySet(),
)

/**
 * The single decision point for "is this app blocked right now".
 *
 * Precedence, highest first:
 * 1. this app and critical system components (phone, emergency, settings, launcher) are never blocked;
 * 2. an explicit "never block" override wins over an explicit "always block" one;
 * 3. an explicit "always block" override;
 * 4. the category selected by [AppCategoryClassifier] is among the user's blocked categories.
 */
object BlockPolicy {

    fun isBlocked(
        packageName: String,
        platformCategory: PlatformCategory,
        rules: BlockRules,
        ownPackageName: String,
        whitelistedPackages: Set<String> = emptySet(),
    ): Boolean {
        if (packageName.isBlank()) return false
        if (packageName == ownPackageName) return false
        if (packageName in whitelistedPackages) return false
        if (packageName in rules.neverBlockedPackages) return false
        if (packageName in rules.alwaysBlockedPackages) return true
        val category = AppCategoryClassifier.classify(packageName, platformCategory)
        return category in rules.blockedCategories
    }
}
