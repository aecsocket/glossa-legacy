package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.*
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
 * Defines style keys for parts of a message.
 * @property default the key of the default style applied.
 * @property args the key of styles applied to arguments.
 */
@ConfigSerializable
data class StylingFormat(
    @Setting(value = "__default__") val default: String? = null,
    @Setting(nodeFromParent = true) val args: Map<List<String>, String> = emptyMap()
) {
    constructor(default: String? = null, vararg args: Pair<List<String>, String>) :
            this(default, args.associate { it })

    companion object {
        /** Format which makes no styling changes. */
        @JvmStatic val IDENTITY = StylingFormat()
    }
}

/**
 * I18N service which uses [StylingFormat]s and [Style]s to localize
 * into Adventure [Component]s.
 */
class StylingI18N(
    locale: Locale = Locale.ROOT
) : AdventureI18N(locale) {
    val styles = HashMap<String, Style>()
    val formats = HashMap<String, StylingFormat>()

    override fun get(locale: Locale, key: String, args: Args) = format(locale, key, args)?.let { lines ->
        val format = formats[key] ?: StylingFormat.IDENTITY
        val defaultStyle = format.default?.let { styles[it] }
        lines.map { line ->
            fun component(token: Templating.FormatToken) =
                text(token.value).defaultStyle(styles[format.args[token.path]])

            if (line.size == 1) {
                component(line[0])
            } else {
                val res = text()
                line.forEach { res.append(component(it)) }
                res.build().defaultStyle(defaultStyle)
            }
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
        child.get(StylingFormat::class)?.let { formats[key.toString()] = it }
    }
    register(loader.load(CONFIG_OPTIONS).req(Translation::class))
}
