package com.example.core.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

enum class AppLanguage(val tag: String, val isRtl: Boolean) {
    ENGLISH(tag = "en", isRtl = false),
    ARABIC(tag = "ar", isRtl = true);

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            val normalized = tag?.lowercase(Locale.ROOT)?.substringBefore("-")
            return entries.firstOrNull { it.tag == normalized } ?: ENGLISH
        }
    }
}

object AppLocaleManager {
    fun setLanguage(language: AppLanguage) {
        val locales = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun getCurrentLanguage(): AppLanguage {
        val activeLocales = AppCompatDelegate.getApplicationLocales()
        val firstTag = if (!activeLocales.isEmpty) {
            activeLocales[0]?.toLanguageTag()
        } else {
            Locale.getDefault().language
        }
        return AppLanguage.fromTag(firstTag)
    }
}
