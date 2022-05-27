package com.github.aecsocket.glossa.core

import java.util.*

class StringI18NBuilder(locale: Locale) : AbstractI18N.Builder<String>(locale) {
    override fun build() = object : AbstractI18N<String, AbstractI18N.TranslationData.Simple>(
        locale,
        buildTranslationData { _, template ->
            TranslationData.Simple(template)
        }
    ) {
        override fun make(
            locale: Locale,
            key: String,
            args: Argument.MapScope<String>.() -> Unit
        ) = translation(key, locale)?.let { data ->
            makeTokens(this, locale, data.template, args).lines.map { line ->
                line.joinToString("") { when (it) {
                    is Token.Text -> it.value
                    is Token.Raw -> it.value
                } }
            }
        }

        override fun safe(
            locale: Locale,
            key: String,
            args: Argument.MapScope<String>.() -> Unit
        ) = make(locale, key, args) ?: listOf(key)
    }
}
