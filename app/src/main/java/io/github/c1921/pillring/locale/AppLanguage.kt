package io.github.c1921.pillring.locale

import android.os.LocaleList
import java.util.Locale

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    CHINESE_SIMPLIFIED;

    fun toLocaleList(): LocaleList {
        return when (this) {
            SYSTEM -> LocaleList.getEmptyLocaleList()
            ENGLISH -> LocaleList(Locale.ENGLISH)
            CHINESE_SIMPLIFIED -> LocaleList(Locale.SIMPLIFIED_CHINESE)
        }
    }

    companion object {
        private const val LANGUAGE_ZH = "zh"

        fun fromLocale(locale: Locale?): AppLanguage {
            val language = locale?.language.orEmpty()
            return if (language.equals(LANGUAGE_ZH, ignoreCase = true)) {
                CHINESE_SIMPLIFIED
            } else {
                ENGLISH
            }
        }
    }
}
