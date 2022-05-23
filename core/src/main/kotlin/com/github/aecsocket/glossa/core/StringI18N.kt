package com.github.aecsocket.glossa.core

import java.util.*

class StringI18N(locale: Locale) : I18N<String>(locale) {
    override fun make(locale: Locale, key: Iterable<String>, args: Argument.MapScope<String>.() -> Unit) =
        template(locale, key)?.let { template ->
            makeTokens(locale, template, args).lines.map { line ->
                line.joinToString("") { when (it) {
                    is Token.Text -> it.value
                    is Token.Raw -> it.value
                } }
            }
        }

    override fun safe(locale: Locale, key: Iterable<String>, args: Argument.MapScope<String>.() -> Unit) =
        make(locale, key, args) ?: listOf(key.tlPath())
}
