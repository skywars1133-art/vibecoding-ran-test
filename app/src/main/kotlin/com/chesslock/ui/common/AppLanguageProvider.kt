package com.chesslock.ui.common

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.chesslock.data.AppLanguage
import java.util.Locale

/**
 * Applies the in-app language choice to every Compose string (SPEC 6). [AppLanguage.SYSTEM]
 * keeps the device locale. This changes only the composition's resources; it does not restart
 * the activity, so switching is immediate.
 *
 * The localized context intentionally wraps the original activity context instead of using
 * `createConfigurationContext` directly: Hilt's `hiltViewModel()` walks the context chain to find
 * the Activity, and a bare configuration context would hide it.
 */
@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val tag = language.languageTag
    if (tag == null) {
        content()
        return
    }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, configuration, tag) {
        val locale = Locale.forLanguageTag(tag)
        val localizedConfiguration = Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val localizedContext = context.createConfigurationContext(localizedConfiguration)
        LocalizedResourcesContext(context, localizedContext.resources) to
            localizedContext.resources.configuration
    }
    CompositionLocalProvider(
        LocalContext provides localized.first,
        LocalConfiguration provides localized.second,
        content = content,
    )
}

/**
 * Non-Compose counterpart of [ProvideAppLanguage] for code that has no composition, such as the
 * home-screen widget. Returns a context whose resources use the chosen language, or the receiver
 * itself for [AppLanguage.SYSTEM].
 */
fun Context.withAppLanguage(language: AppLanguage): Context {
    val tag = language.languageTag ?: return this
    val locale = Locale.forLanguageTag(tag)
    val localizedConfiguration = Configuration(resources.configuration).apply {
        setLocale(locale)
        setLayoutDirection(locale)
    }
    return createConfigurationContext(localizedConfiguration)
}

private class LocalizedResourcesContext(
    base: Context,
    private val localizedResources: Resources,
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResources
}
