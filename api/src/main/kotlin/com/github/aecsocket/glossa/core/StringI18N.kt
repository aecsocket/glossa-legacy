package com.github.aecsocket.glossa.core

import java.util.*

class StringI18N(
    locale: Locale = Locale.ROOT
) : AbstractI18N<List<String>, List<Any>>(locale), MultilineI18N<String, Any> {
    override fun doGet(
        locale: Locale,
        key: String,
        args: Map<String, (I18NContext<List<String>, List<Any>>) -> List<Any>>
    ) = translations[locale]?.get(key)?.let { tl ->
        parse(locale, tl, key, args, { it }, { idx, tokens, post ->
            tokens.joinToString("") { token -> token.pre + token.value[idx] } + post
        }, { it })
    }

    override fun safe(locale: Locale, key: String, args: Map<String, (I18NContext<List<String>, List<Any>>) -> List<Any>>) =
        get(locale, key, args) ?: listOf(key)
}
