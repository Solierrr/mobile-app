package com.project.solaria_mobile.core.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Idiomas do aplicativo; os mesmos do web: pt-BR (padrão), en-US e es-ES. [tag] nulo segue o idioma do aparelho. */
enum class AppLanguage(val tag: String?, val nativeName: String?) {
    SYSTEM(null, null),
    PORTUGUESE("pt-BR", "Português (Brasil)"),
    ENGLISH("en-US", "English"),
    SPANISH("es-ES", "Español"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage = when (tag?.substringBefore('-')?.lowercase()) {
            "pt" -> PORTUGUESE
            "en" -> ENGLISH
            "es" -> SPANISH
            else -> SYSTEM
        }
    }
}

/**
 * Idioma escolhido para o aplicativo, sem depender do idioma do sistema. No Android 13 em diante o sistema guarda a
 * escolha (e a mostra em Configurações → Apps → Idioma); antes disso, ela fica nas preferências e [wrap] a aplica ao
 * abrir a tela.
 */
object AppLocale {
    private const val PREFS = "app_locale"
    private const val KEY = "tag"

    fun saved(context: Context): AppLanguage =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            AppLanguage.fromTag(if (locales.isEmpty) null else locales[0].toLanguageTag())
        } else {
            AppLanguage.fromTag(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
        }

    /** Troca o idioma e recria a tela para o texto mudar na hora. */
    fun apply(activity: Activity, language: AppLanguage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.tag).apply()
            activity.recreate()
        }
    }

    /** Antes do Android 13, aplica o idioma escolhido ao contexto da tela; depois dele o sistema já cuida disso. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return base
        val configuration = android.content.res.Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(tag))
        return base.createConfigurationContext(configuration)
    }
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
