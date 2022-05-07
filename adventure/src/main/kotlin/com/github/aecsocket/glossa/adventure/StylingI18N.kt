package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.api.*
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.util.Locale

/**
 * I18N service which uses [Format]s and [Style]s to localize
 * into Adventure [Component]s.
 */
class StylingI18N(
    locale: Locale = Locale.ROOT
) : AdventureI18N(locale) {
    /**
     * Defines style keys for parts of a message.
     * @property default the key of the default style applied.
     * @property args the key of styles applied to arguments.
     */
    @ConfigSerializable
    data class Format(
        @Setting(value = "__default__") val default: String? = null,
        @Setting(nodeFromParent = true) val args: Map<List<String>, String> = emptyMap()
    ) {
        constructor(default: String? = null, vararg args: Pair<List<String>, String>) :
            this(default, args.associate { it })

        companion object {
            /** Format which makes no styling changes. */
            @JvmStatic val IDENTITY = Format()
        }
    }

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

private val CONFIG_OPTIONS = ConfigurationOptions.defaults()
    .serializers {
        it.registerAll(ConfigurateComponentSerializer.configurate().serializers())
    }
private const val STYLES = "styles"
private const val FORMATS = "formats"

@Throws(ConfigurateException::class)
fun StylingI18N.load(loader: ConfigurationLoader<*>) {
    val node = loader.load(CONFIG_OPTIONS)
    for ((key, child) in node.node(STYLES).childrenMap()) {
         child.get(Style::class)?.let { styles[key.toString()] = it }
    }
    for ((key, child) in node.node(FORMATS).childrenMap()) {
        child.get(StylingI18N.Format::class)?.let { formats[key.toString()] = it }
    }
    register(loader.load(CONFIG_OPTIONS).req(Translation::class))
}
