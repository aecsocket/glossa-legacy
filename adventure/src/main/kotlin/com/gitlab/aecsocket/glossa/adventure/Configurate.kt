package com.gitlab.aecsocket.glossa.adventure

import com.gitlab.aecsocket.glossa.core.*
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import net.kyori.adventure.text.format.Style
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.serialize.SerializationException

private const val STYLES = "styles"
private const val FORMATS = "formats"

private const val STYLE = "style"
private const val ARG_STYLES = "-"

private val configOptions = ConfigurationOptions.defaults()
    .serializers { it.registerAll(ConfigurateComponentSerializer.configurate().serializers()) }

fun MiniMessageI18N.Builder.addFormats(node: ConfigurationNode) {
    val type = MiniMessageI18N.Builder.FormatNode::class.java

    fun buildChildren(node: ConfigurationNode): MutableMap<String, MiniMessageI18N.Builder.FormatNode> {
        val res = HashMap<String, MiniMessageI18N.Builder.FormatNode>()
        node.childrenMap().forEach { (rawKey, child) ->
            val key = try {
                I18NKeys.validate(rawKey.toString())
            } catch (ex: KeyValidationException) {
                throw SerializationException(child, type, "Invalid key", ex)
            }

            res[key] = if (child.isMap) {
                if (child.hasChild(ARG_STYLES)) {
                    // arg styles, style?
                    MiniMessageI18N.Builder.FormatNode(
                        child.node(STYLE).get<String>(),
                        child.node(ARG_STYLES).force<HashMap<String, String>>(),
                        HashMap(),
                    )
                } else {
                    // children
                    MiniMessageI18N.Builder.FormatNode(null, emptyMap(), buildChildren(child))
                }
            } else if (child.isList) {
                // style, children
                val list = child.forceList(type, "style", "children")
                val style = list[0].force<String>()
                val children = buildChildren(list[1])
                MiniMessageI18N.Builder.FormatNode(style, emptyMap(), children)
            } else {
                // style
                MiniMessageI18N.Builder.FormatNode(child.force<String>(), emptyMap(), HashMap())
            }
        }
        return res
    }

    format(MiniMessageI18N.Builder.FormatNode(null, emptyMap(), buildChildren(node)))
}

fun MiniMessageI18N.Builder.fromNode(node: ConfigurationNode) {
    node.node(STYLES).get<Map<String, Style>>()?.forEach { (key, style) ->
        style(key, style)
    }

    addFormats(node.node(FORMATS))

    val tlNode = node.copy().apply {
        removeChild(STYLES)
        removeChild(FORMATS)
    }

    addTranslations(tlNode)
}

fun MiniMessageI18N.Builder.load(loader: ConfigurationLoader<*>) {
    val node = loader.load(configOptions)
    fromNode(node)
}
