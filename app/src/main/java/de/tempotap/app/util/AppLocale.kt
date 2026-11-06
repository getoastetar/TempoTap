package de.tempotap.app.util

import android.content.Context
import android.content.res.Configuration
import de.tempotap.app.data.AppLanguage
import java.util.Locale

/**
 * Wendet die in den Einstellungen gewählte Sprache an.
 *
 * Die Sprache liegt bewusst in SharedPreferences statt in DataStore, weil sie
 * synchron in [Context.attachBaseContext] gelesen werden muss, bevor die
 * Ressourcen der Activity geladen werden.
 */
object AppLocale {

    private const val PREFS_NAME = "app_language"
    private const val KEY_LANGUAGE = "language"

    fun read(context: Context): AppLanguage {
        val tag = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        return AppLanguage.fromTag(tag) ?: AppLanguage.systemDefault()
    }

    fun write(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.languageTag)
            .apply()
        Locale.setDefault(Locale.forLanguageTag(language.languageTag))
    }

    fun wrap(context: Context): Context {
        val locale = Locale.forLanguageTag(read(context).languageTag)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
