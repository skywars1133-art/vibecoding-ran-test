package com.chesslock.domain.blocking

/**
 * Android's `ApplicationInfo.category` translated into a platform-neutral value, so the
 * classifier stays pure Kotlin and testable without the Android SDK.
 */
enum class PlatformCategory {
    SOCIAL,
    GAMES,
    VIDEO,
    AUDIO,
    NEWS,
    MAPS,
    PRODUCTIVITY,
    IMAGE,
    COMMUNICATION,
    SHOPPING,
    UNDEFINED,
}
