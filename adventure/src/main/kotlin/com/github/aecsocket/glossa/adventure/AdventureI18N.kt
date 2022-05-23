package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.Argument
import com.github.aecsocket.glossa.core.I18N
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.Locale

/*class AdventureI18N(
    locale: Locale,
    var miniMessage: MiniMessage = MiniMessage.miniMessage()
) : I18N<Component>(locale) {
    data class Format()

    private val styles = HashMap<String, Style>()
    private val formats = HashMap<String, Format>()

    fun styles(): Map<String, Style> = styles
    fun formats(): Map<String, Format> = formats

    override fun make(key: String, locale: Locale, args: Argument.MapScope<Component>.() -> Unit) =
        template(locale, key)?.let { template ->
            makeTokens(locale, template, args).lines.map { line ->
                val res = text()
                line.forEach { when (it) {
                    is Token.Text -> res.append(miniMessage.deserialize(it.value))
                    is Token.Raw -> res.append(it.value)
                } }
                res.build()
            }
        }

    override fun safe(key: String, locale: Locale, args: Argument.MapScope<Component>.() -> Unit) =
        make(key, locale, args) ?: listOf(text(key))
}*/