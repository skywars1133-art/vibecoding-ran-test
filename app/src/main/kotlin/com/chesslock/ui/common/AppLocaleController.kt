package com.chesslock.ui.common

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.chesslock.data.AppLanguage

/** Locale list used for per-app system resources. Empty means "follow the device". */
internal fun appLocaleList(language: AppLanguage): LocaleList =
    language.languageTag
        ?.let(LocaleList::forLanguageTags)
        ?: LocaleList.getEmptyLocaleList()

/**
 * Applies the in-app language to system-resolved resources too. Compose already follows
 * [ProvideAppLanguage], but launcher/widget labels come from the manifest and need the platform's
 * per-app locale on Android 13+. Older devices keep their system locale for those labels.
 */
internal object AppLocaleController {
    fun apply(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        localeManager.applicationLocales = appLocaleList(language)
    }
}
