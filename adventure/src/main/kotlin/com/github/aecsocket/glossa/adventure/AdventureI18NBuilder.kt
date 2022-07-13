package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.Locale

/**
 * A format for a specific message in an [AdventureI18NBuilder] I18N service.
 * @property style The key of the style for the entire message.
 * @property args The keys for each style for each argument.
 */
data class I18NFormat(
    val style: String?,
    val args: Map<List<String>, String> = emptyMap()
) {
    data class Data(
        val style: Style?,
        val args: Map<List<String>, Style>
    )
}

/**
 * An I18N builder implementation which applies styling and formatting, and generates
 * Adventure components.
 */
class AdventureI18NBuilder(
    locale: Locale
) : AbstractI18N.Builder<Component>(locale) {
    private val miniMessage = MiniMessage.miniMessage()
    private val styles = HashMap<String, Style>()
    private val formats = HashMap<List<String>, I18NFormat>()

    /**
     * Registers a style on this builder.
     */
    fun registerStyle(key: String, style: Style) {
        styles[Keys.validate(key)] = style
    }

    /**
     * Registers a format on this builder.
     */
    fun registerFormat(key: List<String>, format: I18NFormat) {
        formats[key.map { Keys.validate(it) }] = format
    }

    data class Data(
        override val template: Template,
        val format: I18NFormat.Data? = null
    ) : AbstractI18N.TranslationData

    override fun build() = object : AbstractI18N<Component, Data>(
        locale,
        buildTranslationData { path, template ->
            var parentStyle: String? = null
            path.indices.reversed().forEach { i ->
                formats[path.subList(0, i + 1)]?.let { format ->
                    format.style?.let { parentStyle = it }
                    return@forEach
                }
            }

            val rawFormat = formats[path]
            val format = I18NFormat(
                rawFormat?.style ?: parentStyle,
                rawFormat?.args ?: emptyMap()
            )

            Data(template, I18NFormat.Data(
                styles[format.style],
                format.args
                    .map { (key, value) -> styles[value]?.let { key to it } }
                    .filterNotNull()
                    .associate { it })
            )
        }
    ) {
        override fun make(
            locale: Locale,
            key: String,
            args: Argument.MapScope<Component>.() -> Unit
        ) = translation(key, locale)?.let { data ->
            val format = data.format

            fun Component.doStyle(style: Style?): Component = style?.let { applyFallbackStyle(it) } ?: this

            try {
                makeTokens(this, locale, data.template, args)
            } catch (ex: I18NException) {
                i18nException(key, locale, ex)
            }.lines.map { line ->
                val res = text()
                line.forEach {
                    res.append(when (it) {
                        is Token.Text -> miniMessage.deserialize(it.value)
                        is Token.Raw -> it.value
                    }.doStyle(format?.args?.get(it.path)))
                }
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
