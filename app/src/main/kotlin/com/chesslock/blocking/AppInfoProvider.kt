package com.chesslock.blocking

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import com.chesslock.domain.blocking.PlatformCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** One launchable app shown in the per-app blocking picker. */
data class LaunchableApp(val packageName: String, val label: String)

/** Reads Android app metadata and builds the always-allowed set (SPEC 1). */
@Singleton
class AppInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Launchable apps that the user is allowed to block. Critical components (phone, launcher,
     * system settings, this app) are excluded because [BlockPolicy] never blocks them anyway.
     */
    fun launchableApps(): List<LaunchableApp> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packageManager = context.packageManager
        @Suppress("DEPRECATION")
        val activities = packageManager.queryIntentActivities(launcher, 0)
        val whitelisted = whitelistedPackages()
        return activities.asSequence()
            .mapNotNull { it.activityInfo?.applicationInfo }
            .filter { it.packageName !in whitelisted }
            .distinctBy { it.packageName }
            .map {
                LaunchableApp(
                    packageName = it.packageName,
                    label = packageManager.getApplicationLabel(it).toString(),
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }
    fun platformCategory(packageName: String): PlatformCategory {
        val info = try {
            context.packageManager.getApplicationInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return PlatformCategory.UNDEFINED
        }
        return when (info.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> PlatformCategory.SOCIAL
            ApplicationInfo.CATEGORY_GAME -> PlatformCategory.GAMES
            ApplicationInfo.CATEGORY_VIDEO -> PlatformCategory.VIDEO
            ApplicationInfo.CATEGORY_AUDIO -> PlatformCategory.AUDIO
            ApplicationInfo.CATEGORY_NEWS -> PlatformCategory.NEWS
            ApplicationInfo.CATEGORY_MAPS -> PlatformCategory.MAPS
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> PlatformCategory.PRODUCTIVITY
            ApplicationInfo.CATEGORY_IMAGE -> PlatformCategory.IMAGE
            else -> PlatformCategory.UNDEFINED
        }
    }

    /**
     * Phone, emergency, settings, launcher and our own app must stay reachable even if a user
     * override tries to block them. The launcher and dialer are resolved from the device so
     * OEM packages are covered without shipping a huge list.
     */
    fun whitelistedPackages(): Set<String> {
        val packages = mutableSetOf(
            context.packageName,
            "android",
            "com.android.settings",
            "com.android.systemui",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.incallui",
            "com.android.emergency",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
        )
        val packageManager = context.packageManager
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.let(packages::add)
        val dial = Intent(Intent.ACTION_DIAL)
        packageManager.resolveActivity(dial, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.let(packages::add)
        context.getSystemService(TelecomManager::class.java)
            ?.defaultDialerPackage?.let(packages::add)
        return packages
    }
}
