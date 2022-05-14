package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.text.FieldPosition
import java.util.Locale

/**
 * A service to localize a given key based on a locale,
 * generating a [T].
 *
 * Arguments are provided as [A] instances.
 *
 * @param T Generated object type.
 * @param A Arguments type.
 * @property locale Default locale to use, when none is specified, or when a key is not found for a locale.
 */
interface I18N<T, A> {
    var locale: Locale

    /**
     * Generates a [T] localization based on the locale, key and args passed.
     *
     * Returns `null` if the key was not found in the translation or fallback
     * locale's translation.
     *
     * @param locale Locale to generate with.
     * @param key Localization key.
     * @param args Localization arguments.
     * @return Translation.
     */
    operator fun get(locale: Locale, key: String, args: A): T?

    /**
     * Generates a [T] localization based on the key and args passed.
     * Uses the fallback locale [locale].
     *
     * Returns `null` if the key was not found in the translation.
     *
     * @param key Localization key.
     * @param args Localization arguments.
     * @return Translation.
     */
    operator fun get(key: String, args: A) =
        get(locale, key, args)


    /**
     * Generates a [T] localization based on the locale, key and args passed.
     *
     * If the key was not found, generates a default [T] based on the implementation.
     *
     * @param locale Locale to generate with.
     * @param key Localization key.
     * @param args Localization arguments.
     * @return Translation.
     */
    fun safe(locale: Locale, key: String, args: A): T

    /**
     * Generates a [T] localization based on the key and args passed.
     * Uses the fallback locale [locale].
     *
     * If the key was not found, generates a default [T] based on the implementation.
     *
     * @param key Localization key.
     * @param args Localization arguments.
     * @return Translation.
     */
    fun safe(key: String, args: A) =
        safe(locale, key, args)
}

/**
 * Partial implementation of an I18N service.
 *
 * Handles registration of translations.
 */
abstract class AbstractI18N<T, A>(
    override var locale: Locale = Locale.ROOT
) : MutableI18N<T, A> {
    private val translations = HashMap<Locale, Translation>()

    override fun register(tl: Translation) {
        translations[tl.locale]?.putAll(tl) ?: run { translations[tl.locale] = tl.copy() }
    }

    protected fun translation(locale: Locale, key: String) =
        translations[locale]?.get(key) ?: translations[this.locale]?.get(key)

    override fun clear() {
        translations.clear()
    }
}

/**
 * Formats a map of objects.
 * @param args Map of objects.
 * @return Formatted string.
 */
fun MessageFormat.asString(args: Map<String, Any?>): String = StringBuffer().apply {
    this@asString.format(args, this, FieldPosition(0))
}.toString()
