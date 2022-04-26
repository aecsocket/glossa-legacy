package com.github.aecsocket.glossa.core

import java.util.*

class StringI18N(
    locale: Locale = Locale.ROOT
) : AbstractI18N<List<String>>(locale), MultilineI18N<String> {
    override fun doGet(
        locale: Locale,
        key: String,
        args: Map<String, (I18NContext<List<String>>) -> List<String>>
    ) = translations[locale]?.get(key)?.let { tl ->
        parse(locale, tl, key, args, { it }, { idx, tokens, post ->
            tokens.joinToString("") { token -> token.pre + token.value[idx] } + post
        })
    }

    override fun safe(locale: Locale, key: String, args: Map<String, (I18NContext<List<String>>) -> List<String>>) =
        get(locale, key, args) ?: listOf(key)
}
