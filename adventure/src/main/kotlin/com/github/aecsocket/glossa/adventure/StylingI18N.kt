package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.Args
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import java.util.Locale

class StylingI18N(
    locale: Locale = Locale.ROOT
) : AdventureI18N(locale) {
    data class Format(val default: String? = null, val args: Map<List<String>, String> = emptyMap()) {
        constructor(default: String? = null, vararg args: Pair<List<String>, String>) :
            this(default, args.associate { it })

        companion object {
            @JvmStatic val IDENTITY = Format()
        }
    }

    /*
    style: {
      info: { color: "gray" }
      var: { color: "white" }
    }
    format: {
      "message.simple": {
        __default: "info"
        "scope.value": "var"
      }
    }
     */

    val styles = HashMap<String, Style>()
    val formats = HashMap<String, Format>()

    override fun get(locale: Locale, key: String, args: Args) = format(locale, key, args)?.let { lines ->
        val format = formats[key] ?: Format.IDENTITY
        val defaultStyle = format.default?.let { styles[it] }
        lines.map { line ->
            val res = text()
            line.forEach { token ->
                res.append(text(token.value).defaultStyle(styles[format.args[token.path]]))
            }
            res.build().defaultStyle(defaultStyle)
        }
    }
}

private fun Component.defaultStyle(style: Style?) = style?.let { applyFallbackStyle(it) } ?: this
