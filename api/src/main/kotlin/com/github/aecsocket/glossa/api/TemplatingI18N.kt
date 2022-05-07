package com.github.aecsocket.glossa.api

import java.util.*
import kotlin.collections.HashMap

/**
 * Partial implementation of an I18N service which uses the [Templating] methods
 * to perform formatting.
 *
 * Handles caching template nodes and provides utilities to format using [Templating].
 */
abstract class TemplatingI18N<E>(
    locale: Locale = Locale.ROOT
) : AbstractI18N<List<E>, Args>(locale) {
    private val cache = HashMap<Locale, MutableMap<String, Templating.Node?>>()

    protected fun format(locale: Locale, key: String, args: Args): List<List<Templating.FormatToken>>? {
        return cache.computeIfAbsent(locale) { HashMap() }.computeIfAbsent(key) {
            translation(locale, key)?.let { Templating.parse(it) }
        }?.let { node ->
            Templating.format(locale, node, args).lines()
        }
    }

    /**
     * Generates a list of [E] localization based on the locale and key passed.
     * No args are provided.
     *
     * Returns `null` if the key was not found in the translation or fallback
     * locale's translation.
     *
     * @param locale the locale to generate with.
     * @param key the localization key.
     * @return the translation.
     */
    operator fun get(locale: Locale, key: String) = get(locale, key, Args.EMPTY)

    /**
     * Generates a list of [E] localization based on the key passed.
     * Uses the fallback locale [locale].
     * No args are provided.
     *
     * Returns `null` if the key was not found in the translation or fallback
     * locale's translation.
     *
     * @param key the localization key.
     * @return the translation.
     */
    operator fun get(key: String) = get(key, Args.EMPTY)

    /**
     * Generates a list of [E] localization based on the locale and key passed.
     * No args are provided.
     *
     * If the key was not found, generates a default list of [E] based on the implementation.
     *
     * @param locale the locale to generate with.
     * @param key the localization key.
     * @return the translation.
     */
    fun safe(locale: Locale, key: String) = safe(locale, key, Args.EMPTY)

    /**
     * Generates a list of [E] localization based on the key passed.
     * Uses the fallback locale [locale].
     * No args are provided.
     *
     * If the key was not found, generates a default list of [E] based on the implementation.
     *
     * @param key the localization key.
     * @return the translation.
     */
    fun safe(key: String) = safe(key, Args.EMPTY)

    override fun clear() {
        super.clear()
        cache.clear()
    }
}
