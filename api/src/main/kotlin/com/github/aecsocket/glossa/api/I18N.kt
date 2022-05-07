package com.github.aecsocket.glossa.api

import com.ibm.icu.text.MessageFormat
import java.text.FieldPosition
import java.util.Locale

/**
 * An exception that occurs when localizing.
 * @param key the key being localized.
 * @param message the exception message.
 */
open class I18NException(key: String, message: String) :
    RuntimeException("($key) $message")

/**
 * A service to localize a given key based on a locale,
 * generating a [T].
 *
 * Arguments are provided as [A] instances.
 *
 * @property locale the default locale to use, when none is specified,
 * or when a key is not found for a locale.
 */
interface I18N<T, A> {
    var locale: Locale

    /**
     * Generates a [T] localization based on the locale, key and args passed.
     *
     * Returns `null` if the key was not found in the translation or fallback
     * locale's translation.
     *
     * @param locale the locale to generate with.
     * @param key the localization key.
     * @param args the localization arguments.
     * @return the translation.
     */
    operator fun get(locale: Locale, key: String, args: A): T?

    /**
     * Generates a [T] localization based on the key and args passed.
     * Uses the fallback locale [locale].
     *
     * Returns `null` if the key was not found in the translation.
     *
     * @param key the localization key.
     * @param args the localization arguments.
     * @return the translation.
     */
    operator fun get(key: String, args: A) =
        get(locale, key, args)


    /**
     * Generates a [T] localization based on the locale, key and args passed.
     *
     * If the key was not found, generates a default [T] based on the implementation.
     *
     * @param locale the locale to generate with.
     * @param key the localization key.
     * @param args the localization arguments.
     * @return the translation.
     */
    fun safe(locale: Locale, key: String, args: A): T

    /**
     * Generates a [T] localization based on the key and args passed.
     * Uses the fallback locale [locale].
     *
     * If the key was not found, generates a default [T] based on the implementation.
     *
     * @param key the localization key.
     * @param args the localization arguments.
     * @return the translation.
     */
    fun safe(key: String, args: A) =
        safe(locale, key, args)
}

/**
 * A string key-value map, additionally storing what locale this translation
 * is effective for.
 *
 * @property locale the locale this translation is for.
 * @param m the underlying map.
 */
class Translation(val locale: Locale, m: Map<out String, String>) : HashMap<String, String>(m) {
    constructor(locale: Locale) : this(locale, emptyMap())

    /**
     * Deeply copies this translation.
     * @return the copy.
     */
    fun copy() = Translation(locale, this)
}

/**
 * A service to localize a given key based on a locale,
 * generating a [T].
 *
 * Arguments are provided as [A] instances.
 *
 * Translations can be registered and unregistered.
 */
interface MutableI18N<T, A> : I18N<T, A> {
    /**
     * Registers a translation.
     * @param tl the translation.
     */
    fun register(tl: Translation)

    /**
     * Registers a translation.
     * @param locale the translation locale.
     * @param tl the translation pairs.
     */
    fun register(locale: Locale, vararg tl: Pair<String, String>) =
        register(Translation(locale, tl.associate { it }))

    /**
     * Unregisters all translations.
     */
    fun clear()
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
 * @param args The map of objects.
 * @return the formatted string.
 */
fun MessageFormat.asString(args: Map<String, Any?>): String = StringBuffer().apply {
    this@asString.format(args, this, FieldPosition(0))
}.toString()
