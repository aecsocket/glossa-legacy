package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.Locale

class AdventureI18NBuilder(
    locale: Locale,
    var miniMessage: MiniMessage = MiniMessage.miniMessage()
) : AbstractI18N.Builder<Component>(locale) {
    data class Format(val a: String)

    private val styles = HashMap<String, Style>()

    data class Data(
        override val template: Template
    ) : AbstractI18N.TranslationData

    override fun build() = object : AbstractI18N<Component, Data>(
        locale,
        buildTranslationData { Data(it) }
    ) {
        override fun make(
            locale: Locale,
            key: String,
            args: Argument.MapScope<Component>.() -> Unit
        ) = translation(key, locale)?.let { data ->
            makeTokens(this, locale, data.template, args).lines.map { line ->
                val res = text()
                line.forEach { res.append(when (it) {
                    is Token.Text -> miniMessage.deserialize(it.value)
                    is Token.Raw -> it.value
                }) }
                res.build()
            }
        }

        override fun safe(
            locale: Locale,
            key: String,
            args: Argument.MapScope<Component>.() -> Unit
        ) = make(locale, key, args) ?: listOf(text(key))
    }
}
