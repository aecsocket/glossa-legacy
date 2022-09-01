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

fun AbstractI18N.Builder<*, *>.addTranslations(
    node: ConfigurationNode,
    visitor: (List<String>, ConfigurationNode) -> Unit = { _, _ -> }
) {
    val type = TranslationNode.Root::class.java
    if (!node.isMap) throw SerializationException(node, type, "Translations must be expressed as map")

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
                    val lines: List<String> = (if (child.isList) child.force()
                    else listOf(child.force<String>()))

                    visitor(lines, child)

                    children[key] = TranslationNode.Value(lines)
                }
            }
            return children
        }

        translation(TranslationNode.Root(locale, buildChildren(root)))
    }
}
