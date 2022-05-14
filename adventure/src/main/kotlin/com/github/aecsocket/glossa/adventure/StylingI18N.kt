package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.*
import com.github.aecsocket.glossa.core.TemplatingI18N.ArgumentMap
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.util.Locale

/** Serialization key for [StylingFormat.default]. */
const val DEFAULT = "__default__"

/**
 * Defines style keys for parts of a message.
 * @property default Key of the default style applied.
 * @property args Key of styles applied to arguments.
 */
@ConfigSerializable
data class StylingFormat(
    @Setting(value = DEFAULT) val default: String? = null,
    @Setting(nodeFromParent = true) val args: Map<String, String> = emptyMap()
) {
    constructor(default: String? = null, vararg args: Pair<String, String>) :
        this(default, args.associate { it })

    companion object {
        /** Format which makes no styling changes. */
        @JvmStatic val IDENTITY = StylingFormat()
    }
}

/**
 * I18N service which uses [StylingFormat]s and [Style]s to localize
 * into Adventure [Component]s.
 *
 * For [safe] calls, returns a list with the key passed as a [net.kyori.adventure.text.TextComponent].
 *
 * The steps taken for localization are:
 * 1. Look up the format string for the given locale and key, using fallback locale if needed
 * 2. (Cached) Convert the format string to a [TemplateNode] representation
 * 3. Using the [ArgumentMap] and [TemplateNode], generate the lines of [FormatToken]s.
 * 4. For each string token, it is parsed through [miniMessage]'s [MiniMessage.deserialize].
 * 5. For each raw token, it is added to the result.
 */
class StylingI18N(
    locale: Locale = Locale.ROOT,
    val miniMessage: MiniMessage = MiniMessage.miniMessage()
) : AdventureI18N(locale) {
    private val styles = HashMap<String, Style>()
    private val formats = HashMap<String, StylingFormat>()

    fun styles() = styles.toMap()
    fun formats() = formats.toMap()

    fun registerStyle(key: String, style: Style) {
        styles[key] = style
    }

    fun registerFormat(key: String, format: StylingFormat) {
        formats[key] = format
    }

    override fun get(locale: Locale, key: String, args: ArgumentMap<Component>) = format(locale, key, args)?.let { lines ->
        val format = formats[key] ?: StylingFormat.IDENTITY
        val defaultStyle = format.default?.let { styles[it] }
        lines.map { line ->
            val res = line.map { when (it) {
                is StringToken<Component> -> miniMessage.deserialize(it.value)
                is RawToken<Component> -> it.value
            }.defaultStyle(styles[format.args[it.path()]]) }

            if (res.size == 1) {
                res[0].defaultStyle(defaultStyle)
            } else {
                val component = text()
                for (child in res) {
                    // this took so long to debug why tests were failing
                    if (child != empty()) {
                        component.append(child)
                    }
                }
                component.build().defaultStyle(defaultStyle)
            }
        }
    }

    override fun clear() {
        super.clear()
        styles.clear()
        formats.clear()
    }
}

private fun Component.defaultStyle(style: Style?) = style?.let { applyFallbackStyle(it) } ?: this

private val CONFIG_OPTIONS = ConfigurationOptions.defaults()
    .serializers {
        it.registerAll(ConfigurateComponentSerializer.configurate().serializers())
        it.register(Translation::class, Translation.Serializer)
    }
private const val STYLES = "styles"
private const val FORMATS = "formats"

/**
 * Loads styles and formats from a Configurate loader into this I18N.
 *
 * **HOCON example**
 * ```hocon
 * styles: {
 *   info: { color: "gray" } # net.kyori.adventure.text.format.Style
 *   var: { color: "white" }
 * }
 * formats: {
 *   "message.authors": { # com.github.aecsocket.glossa.adventure.StylingFormat
 *     __default__: "info" # default style key
 *     "author": "var" # style key for the `author` scope
 *   }
 * }
 * ```
 * @param loader Loader.
 */
@Throws(ConfigurateException::class)
fun StylingI18N.loadStyling(loader: ConfigurationLoader<*>) {
    val node = loader.load(CONFIG_OPTIONS)
    for ((key, child) in node.node(STYLES).childrenMap()) {
         child.get(Style::class)?.let { registerStyle(key.toString(), it) }
    }
    for ((key, child) in node.node(FORMATS).childrenMap()) {
        child.get(StylingFormat::class)?.let { registerFormat(key.toString(), it) }
    }
}
