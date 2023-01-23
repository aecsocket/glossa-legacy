package com.gitlab.aecsocket.glossa.adventure

import com.gitlab.aecsocket.glossa.core.*
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.serialize.SerializationException

private const val SUBSTITUTIONS = "substitutions"
private const val STYLES = "styles"
private const val KEY_STYLES = "key_styles"
private const val TRANSLATIONS = "translations"

private val configOptions = ConfigurationOptions.defaults()
    .serializers { it.registerAll(ConfigurateComponentSerializer.configurate().serializers()) }

fun MiniMessageI18N.Builder.addSubstitutions(node: ConfigurationNode) {
    fun add(node: ConfigurationNode, path: List<String>) {
        node.childrenMap().forEach { (key, child) ->
            if (child.isMap) {
                add(child, path + key.toString())
            } else {
                substitutions[(path + key).joinToString(PATH_SEPARATOR)] = child.force()
            }
        }
    }

    add(node, emptyList())
}

fun MiniMessageI18N.Builder.addStyles(node: ConfigurationNode) {
    node.childrenMap().forEach { (key, child) ->
        styles[key.toString()] = child.force()
    }
}

fun MiniMessageI18N.Builder.addKeyStyles(node: ConfigurationNode) {
    fun add(node: ConfigurationNode, style: KeyStyleNode) {
        node.childrenMap().forEach { (rawKey, child) ->
            val key = try {
                I18NKeys.validate(rawKey.toString())
            } catch (ex: KeyValidationException) {
                throw SerializationException(child, KeyStyleNode::class.java, "Invalid key", ex)
            }

            if (child.isMap) {
                val newStyle = KeyStyleNode()
                style.node(key, newStyle)
                add(child, newStyle)
            } else {
                style.node(rawKey.toString(), KeyStyleNode(child.force()))
            }
        }
    }

    val root = KeyStyleNode()
    add(node, root)
    keyStyle.mergeFrom(root)
}

fun MiniMessageI18N.Builder.addFromNode(node: ConfigurationNode) {
    addSubstitutions(node.node(SUBSTITUTIONS))
    addStyles(node.node(STYLES))
    addKeyStyles(node.node(KEY_STYLES))
    addTranslations(node.node(TRANSLATIONS))
}

fun MiniMessageI18N.Builder.load(loader: ConfigurationLoader<*>) {
    val node = loader.load(configOptions)
    addFromNode(node)
}
