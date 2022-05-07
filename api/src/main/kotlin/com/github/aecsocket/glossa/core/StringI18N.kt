package com.github.aecsocket.glossa.core

import java.util.*

class StringI18N(
    locale: Locale = Locale.ROOT
) : TemplatingI18N<String>(locale) {
    override fun toE(line: List<Templating.FormatToken>) =
        line.joinToString("") { it.value }

    override fun safe(locale: Locale, key: String, args: Args) =
        get(locale, key, args) ?: listOf(key)
}
