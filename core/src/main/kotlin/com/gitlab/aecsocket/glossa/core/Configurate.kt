package com.gitlab.aecsocket.glossa.core

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type
import java.util.Locale

private const val ROOT = "root"

inline fun <reified T> typeToken() = object : TypeToken<T>() {}

fun <V> ConfigurationNode.force(type: TypeToken<V>) = get(type)
    ?: throw SerializationException(this, type.type, "A value is required for this field")

inline fun <reified V> ConfigurationNode.force() = force(typeToken<V>())

fun ConfigurationNode.forceList(type: Type, vararg args: String): List<ConfigurationNode> {
    if (isList) {
        val list = childrenList()
        if (list.size == args.size)
            return list
        throw SerializationException(this, type, "Field must be expressed as list of [${args.joinToString()}], found ${list.size}")
    }
    throw SerializationException(this, type, "Field must be expressed as list")
}

/**
 * Registers all translations under this node.
 *
 * The node passed must be a map, and each key under this node represents a locale obtained
 * by [Locale.forLanguageTag]. Each child under this node forms a [TranslationNode.Root].
 *
 * @param visitor An operation to apply for each [TranslationNode.Value] created.
 */
fun AbstractI18N.Builder<*, *>.addTranslations(
    node: ConfigurationNode,
    visitor: (TranslationNode.Value, ConfigurationNode) -> Unit = { _, _ -> }
) {
    val type = TranslationNode.Root::class.java
    if (!node.isMap)
        throw SerializationException(node, type, "Translations must be expressed as map")

    node.childrenMap().forEach { (localeKey, root) ->
        val locale = if (localeKey == ROOT) Locale.ROOT
            else Locale.forLanguageTag(localeKey.toString())

        if (!root.isMap) throw SerializationException(node, type, "Translation root must be expressed as map")

        fun buildChildren(node: ConfigurationNode): Map<String, TranslationNode> {
            val children = HashMap<String, TranslationNode>()
            node.childrenMap().forEach { (rawKey, child) ->
                val key = try {
                    I18NKeys.validate(rawKey.toString())
                } catch (ex: KeyValidationException) {
                    throw SerializationException(child, type, "Invalid key", ex)
                }

                if (child.isMap) {
                    children[key] = TranslationNode.Section(buildChildren(child))
                } else {
                    // use MutableList because otherwise the type param becomes `? extends String` not `String`
                    // this breaks the deserializer
                    val lines: List<String> = (if (child.isList) child.force<MutableList<String>>()
                    else listOf(child.force<String>()))

                    val leaf = TranslationNode.Value(lines)
                    visitor(leaf, child)
                    children[key] = leaf
                }
            }
            return children
        }

        translations.add(TranslationNode.Root(locale, buildChildren(root)))
    }
}
