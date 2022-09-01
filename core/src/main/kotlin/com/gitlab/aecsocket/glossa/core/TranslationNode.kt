package com.gitlab.aecsocket.glossa.core

import java.util.Locale

sealed interface TranslationNode {
    val children: Map<String, TranslationNode>

    fun node(key: String) = children[key]

    fun node(path: Iterable<String>): TranslationNode? {
        var cur = this
        path.forEach { cur = cur.node(it) ?: return null }
        return cur
    }

    fun node(vararg path: String) = node(path.asIterable())

    data class Value(val lines: List<String>) : TranslationNode {
        override val children get() = emptyMap<String, TranslationNode>()
    }

    sealed interface Container : TranslationNode {
        operator fun plus(other: TranslationNode): Container
    }

    fun Map<String, TranslationNode>.merge(other: Map<String, TranslationNode>): Map<String, TranslationNode> {
        val res = toMutableMap()
        other.forEach { (key, value) ->
            res[key] = res[key]?.let {
                if (it is Container && value is Container) it + value
                else value
            } ?: value
        }
        return res
    }

    data class Section(
        override val children: Map<String, TranslationNode>,
    ) : Container {
        init {
            children.keys.forEach { I18NKeys.validate(it) }
        }

        override fun plus(other: TranslationNode): Container {
            return Section(children.merge(other.children))
        }
    }

    data class Root(
        val locale: Locale,
        override val children: Map<String, TranslationNode>,
    ) : Container {
        init {
            children.keys.forEach { I18NKeys.validate(it) }
        }

        override fun plus(other: TranslationNode): Root {
            return Root(locale, children.merge(other.children))
        }
    }

    interface Scope {
        fun value(key: String, lines: List<String>)

        fun value(key: String, vararg lines: String) = value(key, lines.asList())

        fun section(key: String, builder: Scope.() -> Unit)
    }
}

private fun TranslationNode.visit(visitor: (TranslationNode, List<String>) -> Unit, path: List<String>) {
    visitor(this, path)
    children.forEach { (key, child) ->
        child.visit(visitor, path + key)
    }
}

fun TranslationNode.visit(visitor: (TranslationNode, List<String>) -> Unit) {
    visit(visitor, emptyList())
}

fun ((TranslationNode.Scope) -> Unit).build(): Map<String, TranslationNode> {
    val res = HashMap<String, TranslationNode>()
    invoke(object : TranslationNode.Scope {
        override fun value(key: String, lines: List<String>) {
            res[key] = TranslationNode.Value(lines)
        }

        override fun section(key: String, builder: TranslationNode.Scope.() -> Unit) {
            res[key] = TranslationNode.Section(builder.build())
        }
    })
    return res
}
