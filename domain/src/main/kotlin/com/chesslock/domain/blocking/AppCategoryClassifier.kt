package com.chesslock.domain.blocking

/**
 * Maps an installed package to one [BlockCategory] (SPEC 1).
 *
 * `ApplicationInfo.category` is only set by some developers, and it has no communication or
 * shopping values, so a curated map of popular packages takes priority. Everything not covered
 * falls back to the platform category and finally to [BlockCategory.OTHER].
 */
object AppCategoryClassifier {

    /** Popular packages with a deliberately chosen category. Unknown packages fall through. */
    private val popularPackages: Map<String, BlockCategory> = mapOf(
        // Social
        "com.facebook.katana" to BlockCategory.SOCIAL,
        "com.instagram.android" to BlockCategory.SOCIAL,
        "com.twitter.android" to BlockCategory.SOCIAL,
        "com.zhiliaoapp.musically" to BlockCategory.SOCIAL,
        "com.ss.android.ugc.trill" to BlockCategory.SOCIAL,
        "com.snapchat.android" to BlockCategory.SOCIAL,
        "com.reddit.frontpage" to BlockCategory.SOCIAL,
        "com.pinterest" to BlockCategory.SOCIAL,
        "com.linkedin.android" to BlockCategory.SOCIAL,
        "com.tumblr" to BlockCategory.SOCIAL,
        "com.discord" to BlockCategory.SOCIAL,
        // Games
        "com.supercell.clashofclans" to BlockCategory.GAMES,
        "com.supercell.clashroyale" to BlockCategory.GAMES,
        "com.supercell.brawlstars" to BlockCategory.GAMES,
        "com.king.candycrushsaga" to BlockCategory.GAMES,
        "com.mojang.minecraftpe" to BlockCategory.GAMES,
        "com.roblox.client" to BlockCategory.GAMES,
        "com.epicgames.fortnite" to BlockCategory.GAMES,
        "com.activision.callofduty.shooter" to BlockCategory.GAMES,
        "com.garena.game.codm" to BlockCategory.GAMES,
        "com.tencent.ig" to BlockCategory.GAMES,
        "com.dts.freefireth" to BlockCategory.GAMES,
        "com.ea.gp.fifamobile" to BlockCategory.GAMES,
        "com.rovio.baba" to BlockCategory.GAMES,
        "com.imangi.templerun2" to BlockCategory.GAMES,
        // Video / streaming
        "com.google.android.youtube" to BlockCategory.VIDEO,
        "com.google.android.apps.youtube.kids" to BlockCategory.VIDEO,
        "com.netflix.mediaclient" to BlockCategory.VIDEO,
        "com.amazon.avod.thirdpartyclient" to BlockCategory.VIDEO,
        "com.disney.disneyplus" to BlockCategory.VIDEO,
        "com.hbo.hbonow" to BlockCategory.VIDEO,
        "com.wbd.stream" to BlockCategory.VIDEO,
        "com.crunchyroll.crunchyroid" to BlockCategory.VIDEO,
        "tv.twitch.android.app" to BlockCategory.VIDEO,
        "com.google.android.videos" to BlockCategory.VIDEO,
        // Communication
        "com.whatsapp" to BlockCategory.COMMUNICATION,
        "com.whatsapp.w4b" to BlockCategory.COMMUNICATION,
        "org.telegram.messenger" to BlockCategory.COMMUNICATION,
        "com.facebook.orca" to BlockCategory.COMMUNICATION,
        "com.viber.voip" to BlockCategory.COMMUNICATION,
        "com.google.android.apps.messaging" to BlockCategory.COMMUNICATION,
        "com.microsoft.teams" to BlockCategory.COMMUNICATION,
        "com.slack" to BlockCategory.COMMUNICATION,
        "com.zoom.videomeetings" to BlockCategory.COMMUNICATION,
        "com.skype.raider" to BlockCategory.COMMUNICATION,
        "com.tencent.mm" to BlockCategory.COMMUNICATION,
        "com.google.android.gm" to BlockCategory.COMMUNICATION,
        // Shopping
        "com.amazon.mShop.android.shopping" to BlockCategory.SHOPPING,
        "com.ebay.mobile" to BlockCategory.SHOPPING,
        "com.aliexpress.buyer" to BlockCategory.SHOPPING,
        "com.contextlogic.wish" to BlockCategory.SHOPPING,
        "com.etsy.android" to BlockCategory.SHOPPING,
        "com.zappos.android" to BlockCategory.SHOPPING,
        "com.temu" to BlockCategory.SHOPPING,
        "com.shein" to BlockCategory.SHOPPING,
    )

    fun classify(packageName: String, platformCategory: PlatformCategory): BlockCategory {
        popularPackages[packageName]?.let { return it }
        return when (platformCategory) {
            PlatformCategory.SOCIAL -> BlockCategory.SOCIAL
            PlatformCategory.GAMES -> BlockCategory.GAMES
            PlatformCategory.VIDEO -> BlockCategory.VIDEO
            PlatformCategory.COMMUNICATION -> BlockCategory.COMMUNICATION
            PlatformCategory.SHOPPING -> BlockCategory.SHOPPING
            PlatformCategory.AUDIO,
            PlatformCategory.NEWS,
            PlatformCategory.MAPS,
            PlatformCategory.PRODUCTIVITY,
            PlatformCategory.IMAGE,
            PlatformCategory.UNDEFINED,
            -> BlockCategory.OTHER
        }
    }

    /** The curated category for [packageName], independent of the platform metadata. */
    fun popularCategory(packageName: String): BlockCategory? = popularPackages[packageName]
}
