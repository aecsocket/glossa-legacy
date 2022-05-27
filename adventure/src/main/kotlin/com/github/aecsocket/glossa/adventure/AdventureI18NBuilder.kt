package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.Locale

data class I18NFormat(
    val style: String
) {
    data class Data(
        val style: Style?
    )
}

class AdventureI18NBuilder(
    locale: Locale
) : AbstractI18N.Builder<Component>(locale) {
    private val miniMessage = MiniMessage.miniMessage()
    private val styles = HashMap<String, Style>()
    private val formats = HashMap<List<String>, I18NFormat>()

    fun registerStyle(key: String, style: Style) {
        styles[key.validate()] = style
    }

    fun registerFormat(key: List<String>, format: I18NFormat) {
        formats[key.map { it.validate() }] = format
    }

    data class Data(
        override val template: Template,
        val format: I18NFormat.Data? = null
    ) : AbstractI18N.TranslationData

    override fun build() = object : AbstractI18N<Component, Data>(
        locale,
        buildTranslationData { path, template ->
            var style: String? = null
            for (i in path.indices) {
                formats[path.subList(0, i)]?.let { style = it.style }
            }
            Data(template, I18NFormat.Data(styles[style]))
        }
    ) {
        override fun make(
            locale: Locale,
            key: String,
            args: Argument.MapScope<Component>.() -> Unit
        ) = translation(key, locale)?.let { data ->
            val format = data.format
            makeTokens(this, locale, data.template, args).lines.map { line ->
                val res = text()
                line.forEach { res.append(when (it) {
                    is Token.Text -> miniMessage.deserialize(it.value)
                    is Token.Raw -> it.value
                }) }
                val component = res.build()
                format?.let { fmt -> fmt.style?.let { component.applyFallbackStyle(it) } } ?: component
            }
        }

        override fun safe(
            locale: Locale,
            key: String,
            args: Argument.MapScope<Component>.() -> Unit
        ) = make(locale, key, args) ?: listOf(text(key))
    }
}
