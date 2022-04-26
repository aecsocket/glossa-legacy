package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.MultilineI18N
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import java.util.Locale

class StylingI18N(
    locale: Locale = Locale.ROOT
) : AdventureI18N(locale) {
    data class Format(val default: String? = null, val args: Map<String, String> = emptyMap()) {
        companion object {
            @JvmStatic val IDENTITY = Format()
        }
    }

    private val styles = HashMap<String, Style>()
    private val formats = HashMap<String, Format>()

    override fun doGet(
        locale: Locale,
        key: String,
        args: Map<String, () -> List<Component>>
    ) = translations[locale]?.get(key)?.let { tl ->
        val format = formats[key] ?: Format.IDENTITY
        val defaultStyle = format.default?.let { styles[it] }

        MultilineI18N.parse(tl, key, args, {
            text(it).style(defaultStyle)
        }, { idx, tokens, post ->
            val result = text()
            tokens.forEach { token -> result
                .append(text(token.pre))
                .append(token.value[idx].style(styles[format.args[token.key]]))
            }
            (result + text(post)).build()
        })
    }
}

private fun Component.style(style: Style?) = style?.let { applyFallbackStyle(it) } ?: this
