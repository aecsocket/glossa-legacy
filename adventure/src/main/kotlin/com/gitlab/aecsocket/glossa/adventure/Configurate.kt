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

private val configOptions = ConfigurationOptions.defaults()
    .serializers { it.registerAll(ConfigurateComponentSerializer.configurate().serializers()) }

fun MiniMessageI18N.Builder.addStyles(node: ConfigurationNode) {
    node.get<Map<String, Style>>()?.forEach { (key, style) ->
        style(key, style)
    }
}

fun MiniMessageI18N.Builder.addFormats(node: ConfigurationNode) {
    val type = MiniMessageI18N.Builder.FormatNode::class.java

    val rootFormat = MiniMessageI18N.Builder.FormatNode(null, HashMap(), HashMap())
    node.childrenMap().forEach { (rawKey, child) ->
        val path = rawKey.toString().split(PATH_SEPARATOR).map {
            try {
                I18NKeys.validate(it)
            } catch (ex: KeyValidationException) {
                throw SerializationException(child, type, "Invalid key", ex)
            }
        }

        fun argStylesOf(node: ConfigurationNode) = node.childrenMap()
            .map { (key, style) -> key.toString() to style.force<String>() }
            .associate { it }
            .toMutableMap()

        val format = rootFormat.forceNode(path)
        if (child.isMap) {
            format.argStyles = argStylesOf(child)
        } else if (child.isList) {
            val list = child.forceList(type, "style", "arg_styles")
            format.style = list[0].force()
            format.argStyles = argStylesOf(list[1])
        } else {
            format.style = child.force()
        }
    }

    format(rootFormat)
}

fun MiniMessageI18N.Builder.addFromNode(node: ConfigurationNode) {
    addStyles(node.node(STYLES))
    addFormats(node.node(FORMATS))

    val tlNode = node.copy().apply {
        removeChild(STYLES)
        removeChild(FORMATS)
    }

    addTranslations(tlNode)
}

fun MiniMessageI18N.Builder.load(loader: ConfigurationLoader<*>) {
    val node = loader.load(configOptions)
    addFromNode(node)
}
