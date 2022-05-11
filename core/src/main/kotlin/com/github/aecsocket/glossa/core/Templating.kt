package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import kotlin.collections.ArrayList

/**
 * An immutable parsed version of a message.
 */
sealed interface TemplateNode {
    /**
     * Gets the value of this node in a rendered tree.
     * @return the lines of the value.
     */
    fun treeValue(): List<String>
}

/**
 * Renders out this node using [Trees.render].
 * @return the lines of the render.
 */
fun TemplateNode.renderTree(): List<String> = if (this is ContainerNode) Trees.render(children,
    { it.treeValue() },
    { it.renderTree() }) else emptyList()

/**
 * A node holding raw text.
 * @property value the text.
 */
data class TextNode(
    val value: String
) : TemplateNode {
    override fun treeValue() = value.split('\n').map { "<$it>" }

    override fun toString() = "\"$value\""
}

/**
 * A node defining a substitution.
 * @property key the label of the substitution.
 */
data class SubstitutionNode(
    val key: String,
    val separator: String
) : TemplateNode {
    override fun treeValue() = listOf("@\$<$key> [${separator.split("\n").joinToString(" | ")}]")

    override fun toString() = "@\$<$key>"
}

/**
 *
 * @property children the children of this node.
 */
sealed interface ContainerNode : TemplateNode {
    val children: List<TemplateNode>

    /**
     * Appends another node to this node's children.
     * @param node the child to add.
     * @return the resulting node.
     */
    operator fun plus(node: TemplateNode): TemplateNode
}

data class RootNode(
    override val children: List<TemplateNode> = emptyList()
) : ContainerNode {
    override fun plus(node: TemplateNode) = RootNode(children + node)

    override fun treeValue() = listOf("<container>")
}

/**
 * A node defining a scope block enter.
 * @property key the key of the argument of this scope.
 * @property separator the text of the child separator.
 */
data class ScopeNode(
    val key: String,
    val separator: String,
    override val children: List<TemplateNode> = emptyList()
) : ContainerNode {
    override fun plus(node: TemplateNode) = ScopeNode(key, separator, children + node)

    override fun treeValue() = listOf("@<$key> [${separator.split("\n").joinToString(" | ")}]")

    override fun toString() = "@<$key>$children"
}

/** Bracket enter. */
const val ENTER = '['
/** Bracket exit. */
const val EXIT = ']'
/** Regex pattern for a special token. */
const val PATTERN = "(@\\\$?)<([a-z0-9_]+)>(\\[)?"
const val TOKEN_SCOPE = "@"
const val TOKEN_SUBSTITUTION = "@$"

/** Key for elements which are separators of other tokens. */
const val SEPARATOR = "__separator__"

/**
 * Helper class for templating string messages using a custom scope
 * block format, combined with Unicode ICU [MessageFormat]s.
 *
 * See [parseChildren], [format] for parsing and formatting detail.
 */
object Templating {
    private fun parseChildren(format: String, startIndex: Int = 0): List<TemplateNode> {
        fun MutableList<TemplateNode>.addText(value: String) {
            if (value.isNotEmpty()) {
                add(TextNode(value))
            }
        }

        val res = ArrayList<TemplateNode>()
        Regex(PATTERN).find(format, startIndex)?.let { match ->
            res.addText(format.substring(startIndex, match.range.first))
            val key = match.groups[2]!!.value

            val idx = when (val type = match.groups[1]?.value) {
                TOKEN_SCOPE -> {
                    // @<key>[...]
                    format.countEnterExit(ENTER, EXIT,
                        { content, i ->
                            val (separator, idx) = if (i + 1 < format.length && format[i + 1] == ENTER)
                                format.countEnterExit(ENTER, EXIT,
                                    { separator, i2 -> separator to i2 },
                                    { _, _ -> "" to i }, // if we don't terminate separator properly, it's just more content
                                    i + 2
                                )
                            else "" to i

                            res.add(ScopeNode(key, separator, parseChildren(content)))
                            idx + 1
                        },
                        { _, idx ->
                            // Lenient method (here): just parse the remainder
                            //   Note that we can't use `match.range.first` here,
                            //   since that would lead to it trying to reparse this symbol,
                            //   causing a stack overflow
                            // Strict method: throw an unbalanced brackets exception
                            idx
                        },
                        match.range.last + 1
                    )
                }
                TOKEN_SUBSTITUTION -> {
                    // @$<key>[...]
                    val (separator, idx) = if (match.groups.size == 4) // has [...]
                            format.countEnterExit(ENTER, EXIT,
                            { content, i -> content to i+1 },
                            { _, i -> "" to i },
                            match.range.last + 1
                        ) else // does not have [...]
                            "" to match.range.last
                    res.add(SubstitutionNode(key, separator))
                    idx
                }
                else -> throw IllegalStateException("match with type $type")
            }

            res.addAll(parseChildren(format, idx))
        } ?: run {
            res.addText(format.substring(startIndex))
        }
        return res
    }

    // TODO update javadoc
    /**
     * Parses out a format string into its constituent nodes.
     *
     * Scope block enter/exits are defined by [PATTERN] and [EXIT],
     * where the text between `<>` defines the key,
     * the text between the first set of brackets defines the content,
     * and the optional text between the last set of brackets defines the separator.
     *
     * Substitutions are defined by [PATTERN],
     * where the text between `<>` defines the key,
     * and the optional text between the brackets defines the separator.
     *
     * **Format**
     *
     *     Actions on {date, date, short}: @<action>[
     *       Purchases: {purchases, number} @<entry>[
     *         - "@$<item>[, ]" x{amount}]]
     *
     * **Result**
     *
     *     ├─ <Actions on {date, date, short}: >
     *     └─ @<action> []
     *         ├─ <>
     *         │  <  Purchases: {purchases, number} >
     *         └─ @<entry> []
     *             ├─ <>
     *             │  <    - ">
     *             ├─ @$<item> [, ]
     *             └─ <" x{amount}>
     *
     * @param format the string format.
     * @param startIndex the index in the string from which to start.
     * @return the resulting node.
     */
    fun parse(format: String, startIndex: Int = 0): TemplateNode {
        val res = parseChildren(format, startIndex)
        return if (res.size == 1) res[0] else RootNode(res)
    }
}

private fun <T> CharSequence.countEnterExit(
    enter: Char,
    exit: Char,
    onExit: (String, Int) -> T,
    onRemainder: (String, Int) -> T,
    startIndex: Int = 0
): T {
    var depth = 1
    val content = StringBuilder()
    for (i in startIndex until length) {
        when (val ch = get(i)) {
            enter -> {
                depth++
                content.append(ch)
            }
            exit -> {
                depth--
                if (depth == 0) {
                    return onExit(content.toString(), i)
                } else {
                    content.append(ch)
                }
            }
            else -> content.append(ch)
        }
    }
    return onRemainder(content.toString(), startIndex)
}
