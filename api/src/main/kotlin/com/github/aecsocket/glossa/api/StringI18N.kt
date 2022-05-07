package com.github.aecsocket.glossa.api

import java.util.*

/**
 * I18N service which localizes to a list of strings.
 *
 * For [safe] calls, returns a list with the key passed as a single element.
 */
class StringI18N(
    locale: Locale = Locale.ROOT
) : TemplatingI18N<String>(locale) {
    override fun get(locale: Locale, key: String, args: Args) = format(locale, key, args)?.let { lines ->
        lines.map { line -> line.joinToString("") { it.value } }
    }

    override fun safe(locale: Locale, key: String, args: Args) =
        get(locale, key, args) ?: listOf(key)
}
