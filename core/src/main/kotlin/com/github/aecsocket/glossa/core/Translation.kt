package com.github.aecsocket.glossa.core

import java.util.Locale

/**
 * A node in a translation structure.
 */
sealed interface Translation {
    /** This node's children. */
    val children: Map<String, Translation>

    /**
     * Gets a child in this node structure.
     */
    operator fun get(path: Iterable<String>): Translation? {
        var cur = this
        path.forEach {
            cur = cur.children[it] ?: return null
        }
        return cur
    }

    /**
     * Gets a child in this node structure.
     */
    operator fun get(vararg path: String) = get(path.asIterable())

    /**
     * Recursively iterates over all nodes in this structure.
     */
    fun walk(action: (List<String>, Translation) -> Unit, path: List<String> = emptyList()) {
        children.forEach { (key, child) ->
            val newPath = path + key
            action(newPath, child)
            child.walk(action, newPath)
        }
    }

    /**
     * Recursively iterates over all nodes in this structure.
     */
    fun walk(action: (List<String>, Translation) -> Unit) = walk(action, emptyList())

    /**
     * A leaf node in a translation tree that holds a translation.
     * This node cannot hold any children.
     */
    data class Value(val value: String) : Translation {
        override val children: Map<String, Translation>
            get() = emptyMap()
    }

    /**
     * A node that can hold child nodes.
     */
    interface Parent : Translation {
        /**
         * Merges this translation with another, prioritising the other.
         */
        operator fun plus(other: Translation): Parent
    }

    /**
     * Merges this translation with another, prioritising the other.
     */
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

    /**
     * A root node in a translation tree, holding the locale this tree is for.
     * @property locale The locale of the translation.
     */
    data class Root(
        val locale: Locale,
        override val children: Map<String, Translation>
    ) : Parent {
        init {
            children.keys.forEach { Keys.validate(it) }
        }

        override fun plus(other: Translation): Root {
            return Root(locale, children.merge(other.children))
        }
    }

    /**
     * A node in a translation tree, holding child nodes.
     */
    data class Section(override val children: Map<String, Translation>) : Parent {
        init {
            children.keys.forEach { Keys.validate(it) }
        }

        override fun plus(other: Translation): Parent {
            return Section(children.merge(other.children))
        }
    }

    /**
     * A DSL scope for building a translation.
     */
    interface Scope {
        fun value(key: String, value: String)

        fun section(key: String, content: Scope.() -> Unit)
    }

    companion object {
        /**
         * Builds translation node children from a scope.
         */
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

        /**
         * Builds a translation root from a scope.
         */
        fun buildRoot(locale: Locale, content: Scope.() -> Unit) = Root(locale, buildChildren(content))
    }
}
