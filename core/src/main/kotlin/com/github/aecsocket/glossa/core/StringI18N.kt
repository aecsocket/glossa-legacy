package com.github.aecsocket.glossa.core

import com.github.aecsocket.glossa.core.TemplatingI18N.ArgumentMap
import java.util.*

/**
 * I18N service which localizes to a list of strings.
 *
 * For [safe] calls, returns a list with the key passed as a single element.
 *
 * The steps taken for localization are:
 * 1. Look up the format string for the given locale and key, using fallback locale if needed
 * 2. (Cached) Convert the format string to a [TemplateNode] representation
 * 3. Using the [ArgumentMap] and [TemplateNode], generate the lines of [FormatToken]s.
 * 4. These are converted to string lists.
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
