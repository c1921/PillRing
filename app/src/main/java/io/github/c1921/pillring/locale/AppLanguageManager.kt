package io.github.c1921.pillring.locale

import android.app.LocaleManager
import android.content.Context
import java.util.Locale

object AppLanguageManager {
    fun getSelectedLanguage(context: Context): AppLanguage {
        val applicationLocales = localeManager(context).applicationLocales
        if (applicationLocales.isEmpty) {
            return AppLanguage.SYSTEM
        }
        return AppLanguage.fromLocale(applicationLocales[0])
    }

    fun applyLanguage(
        context: Context,
        language: AppLanguage
    ): Boolean {
        val localeManager = localeManager(context)
        val targetLocales = language.toLocaleList()
        if (localeManager.applicationLocales == targetLocales) {
            return false
        }

        localeManager.applicationLocales = targetLocales
        return true
    }

    fun getEffectiveLanguageForSummary(context: Context): AppLanguage {
        val selected = getSelectedLanguage(context)
        if (selected != AppLanguage.SYSTEM) {
            return selected
        }

        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        return AppLanguage.fromLocale(locale)
    }

    private fun localeManager(context: Context): LocaleManager {
        return context.getSystemService(LocaleManager::class.java)
    }
}
