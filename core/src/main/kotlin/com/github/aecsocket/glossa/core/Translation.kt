package com.github.aecsocket.glossa.core

import java.util.Locale

sealed interface Translation {
    val children: Map<String, Translation>

    operator fun get(path: Iterable<String>): Translation? {
        var cur = this
        path.forEach {
            cur = cur.children[it] ?: return null
        }
        return cur
    }

    operator fun get(vararg path: String) = get(path.asIterable())

    data class Value(val value: String) : Translation {
        override val children: Map<String, Translation>
            get() = emptyMap()
    }

    interface Parent : Translation {
        operator fun plus(other: Translation): Parent
    }

    fun Map<String, Translation>.merge(other: Map<String, Translation>): Map<String, Translation> {
        val res = toMutableMap()
        other.forEach { (key, value) ->
            res[key] = res[key]?.let {
                // merge, else replace with other's
                if (it is Parent && value is Parent) it + value
                else value
            } ?: value
        }
        return res
    }

    data class Root(
        val locale: Locale,
        override val children: Map<String, Translation>
    ) : Parent {
        override fun plus(other: Translation): Root {
            return Root(locale, children.merge(other.children))
        }
    }

    data class Section(override val children: Map<String, Translation>) : Parent {
        override fun plus(other: Translation): Parent {
            return Section(children.merge(other.children))
        }
    }

    interface Scope {
        fun value(key: String, value: String)

        fun section(key: String, content: Scope.() -> Unit)
    }

    companion object {
        fun buildChildren(content: Scope.() -> Unit): Map<String, Translation> {
            val res = HashMap<String, Translation>()
            content(object : Scope {
                override fun value(key: String, value: String) {
                    res[key] = Value(value)
                }

                override fun section(key: String, content: Scope.() -> Unit) {
                    res[key] = Section(buildChildren(content))
                }
            })
            return res
        }

        fun buildRoot(locale: Locale, content: Scope.() -> Unit) = Root(locale, buildChildren(content))
    }
}

const val TL_SEPARATOR = "."

fun Iterable<String>.tlPath() = joinToString(TL_SEPARATOR)

/*
data class Translation(
    val locale: Locale,
    val map: Map<String, String>
) {
    operator fun get(key: String) = map[key]

    operator fun plus(other: Map<String, String>) = Translation(locale, map + other)

    operator fun plus(other: Translation) = Translation(locale, map + other.map)

    object Serializer : TypeSerializer<Translation> {
        private const val LOCALE = "__locale__"

        override fun serialize(type: Type, obj: Translation?, node: ConfigurationNode) {
            if (obj == null) node.set(null)
            else {
                node.set(obj.map)
                node.node(LOCALE).set(obj.locale)
            }
        }

        override fun deserialize(type: Type, node: ConfigurationNode) = Translation(
            Locale.forLanguageTag(node.node(LOCALE).req()),
            node.childrenMap()
                .filterKeys { it != LOCALE }
                .map { (key, value) ->
                    key.toString() to if (value.isList)
                        value.req<Array<String>>().joinToString("\n")
                    else value.req()
                }.associate { it }
        )
    }
}*/
