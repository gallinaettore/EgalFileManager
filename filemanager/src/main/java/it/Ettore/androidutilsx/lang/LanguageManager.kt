package it.Ettore.androidutilsx.lang

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.preference.PreferenceManager
import java.util.*


/*
Copyright (c)2019 - Egal Net di Ettore Gallina
*/





/**
 * Classe per la gestione delle lingue
 */
class LanguageManager(val context: Context, val lingue: List<Lingua>) {

    companion object {
        const val KEY_LANGUAGE = "language"
        const val DEFAULT_LANGUAGE = "English"
        private const val CODICE_LOCALE_DEFAULT = "en"
    }



    /**
     * Nomi delle lingue ordinate
     */
    val nomiLingue : List<String> = lingue.sorted().map { it.nome }


    /**
     * Codici codiceLocale delle lingue (es. "it", "pr_PT") ordinate in modo da corrispondere ai nomi delle lingue
     */
    val codiciLocaleLingue : List<String> = lingue.sorted().map { it.codiceLocale }



    /**
     * Cerca la lingua che contiene un determinato locale
     * @param locale Locale che la lingua deve contenere (in formato lingua o in formato lingua e paese)
     * @return Lingua che contiene il locale. Null se non viene trovata alcuna lingua per quel locale
     */
    private fun findLangByLocale(locale: Locale): Lingua? {
        val linguaECountryDispositivo = "${locale.language}_${locale.country}"
        var lingua: Lingua? = lingue.find { it.codiceLocale == linguaECountryDispositivo }
        if(lingua == null){
            lingua = lingue.find { it.codiceLocale == locale.language }
        }
        if(lingua == null){
            lingua = lingue.find { soloLingua(it.codiceLocale) == locale.language }
        }
        return lingua
    }


    /**
     * Trasforma un codice locale in oggetto della classe Locale
     * @param localeCode Codice locale (in formato "pt" o "pt_PT" o "pt-PT")
     * @return Locale corrispondente al codice
     */
    private fun localeCodeToLocale(localeCode: String): Locale {
        val code = localeCode.replace("-", "_")
        return if(!code.contains("_")){
            Locale(code)
        } else {
            val split = code.split("_")
            Locale(split[0], split[1])
        }
    }


    /**
     * Estrae solo il codice della lingua da un codice codiceLocale completo
     * @param locale Striga che rappresenta un codiceLocale in formato "en_US" o "en-US" in formato "en"
     * @return Codice lingua (es. "en")
     */
    private fun soloLingua(locale: String): String {
        val code = locale.replace("-", "_")
        val index = code.indexOf("_")
        return if(index != -1) code.substring(0, index) else locale
    }


    /**
     * Imposta il locale dell'applicazione
     * @param codiceLocale Codice del locale in formato "pt" o "pt_PT" o "pt-PT"
     * @return ContextWrapper da usare nel metodo attachBaseContext
     */
    fun changeLocale(codiceLocale: String?): ContextWrapper {
        if(codiceLocale == null) return ContextWrapper(context)
        val lingua: Lingua = findLangByLocale(localeCodeToLocale(codiceLocale)) ?: return ContextWrapper(context)
        return setApplicationLocale(localeCodeToLocale(lingua.codiceLocale))
    }


    /**
     * Imposta il locale dell'applicazione leggendo il codice locale settato lelle preferences
     * @return ContextWrapper da usare nel metodo attachBaseContext
     */
    fun changeLocaleFromPreferences(): ContextWrapper {
        return changeLocale(getSettedApplicationLocaleCode())
    }


    /**
     * Imposta il locale dell'applicazione
     * @param nomeLingua Nome della lingua (contenuto nella lista lingue. es. "Italian", "Croatian"...)
     */
    fun changeLanguage(nomeLingua: String): ContextWrapper {
        val lingua: Lingua? = lingue.find { it.nome == nomeLingua }
        return changeLocale(lingua?.codiceLocale)
    }


    /**
     * Restituisce la lingua settata nelle preferences dell'applicazione
     * @return Lingua settata nell'applicazione. Null se non è stata settata alcuna lingua o se il valore settato non corrisponde ad alcuna lingua
     */
    private fun getSettedApplicationLanguage() : Lingua? {
        val codiceLocaleSettato = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_LANGUAGE, null)
        return if (codiceLocaleSettato != null){
            findLangByLocale(localeCodeToLocale(codiceLocaleSettato))
        } else {
            null
        }
    }


    /**
     * Restituisce il codice della lingua settata nelle preferences dell'applicazione nel formato "pt" o "pt_PT"
     * Restituisce null se non è stata settata alcuna lingua o se il valore settato non corrisponde ad alcuna lingua
     */
    private fun getSettedApplicationLocaleCode() : String? {
        val lingua = getSettedApplicationLanguage()
        return lingua?.codiceLocale
    }


    /**
     * Imposta il codiceLocale dell'applicazione
     */
    private fun setApplicationLocale(locale: Locale): ContextWrapper {
        val conf = context.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            conf.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            conf.locale = locale
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val newContext = context.createConfigurationContext(conf)
            ContextWrapper(newContext)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(conf, context.resources.displayMetrics)
            ContextWrapper(context)
        }
    }


    /**
     * Restituisce il codiceLocale settato nel context dell'applicazione
     */
    fun getApplicationLocale(): Locale {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            return context.resources.configuration.locale
        }
    }


    /**
     * Restituisce la lingua settata nel context dell'applicazione,
     * se la lingua settata non esiste (perchè magari è stata modificata) restituisce la lingua di default Inglese
     */
    fun getCurrentAppLanguage(): Lingua {
        var lingua: Lingua? = findLangByLocale(getApplicationLocale())
        if(lingua == null){
            lingua = findLangByLocale(Locale(CODICE_LOCALE_DEFAULT))
        }
        return lingua!!
    }
}