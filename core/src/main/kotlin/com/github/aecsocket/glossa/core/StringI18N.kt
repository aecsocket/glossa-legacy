package com.github.aecsocket.glossa.core

import java.util.*

/**
 * I18N service which localizes to a list of strings.
 *
 * For [safe] calls, returns a list with the key passed as a single element.
 */
class StringI18N(
    locale: Locale = Locale.ROOT
) : TemplatingI18N<String>(locale) {
    override fun get(locale: Locale, key: String, args: ArgumentMap<String>) = format(locale, key, args)?.let { lines ->
        lines.map { line -> line.joinToString("") {
            when (it) {
                is StringToken<String> -> it.value
                is RawToken<String> -> it.value
            }
        } }
    }

    override fun safe(locale: Locale, key: String, args: ArgumentMap<String>) =
        get(locale, key, args) ?: listOf(key)
}
