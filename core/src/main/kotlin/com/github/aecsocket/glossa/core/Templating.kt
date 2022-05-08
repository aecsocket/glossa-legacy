package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Arguments for [Templating.format].
 * @property args the underlying arguments.
 */
data class Args(val args: Map<String, () -> Any>) {
    constructor(vararg args: Pair<String, () -> Any>) :
            this(args.associate { it })

    /**
     * Gets the argument with the specified key.
     * @param key the key.
     * @return the value.
     */
    operator fun get(key: String) = args[key]

    /**
     * Combines these arguments with another instance.
     * @param o the other arguments.
     * @return the resulting arguments.
     */
    operator fun plus(o: Args) = Args(args + o.args)

    /**
     * Adds another key-value pair to these arguments.
     * @param arg the new pair.
     * @return the resulting arguments.
     */
    operator fun plus(arg: Pair<String, () -> Any>) = Args(args + arg)

    /**
     * Runs an action for each element of these arguments.
     * @param action the action.
     */
    fun forEach(action: (String, () -> Any) -> Unit) = args.forEach(action)

    companion object {
        /**
         * An instance with no arguments.
         */
        @JvmStatic val EMPTY = Args(emptyMap())
    }
}

/**
 * Collection of arguments for [Templating.format].
 * @property args the underlying arguments.
 */
data class MultiArgs(val args: Collection<() -> Any>) {
    constructor(vararg args: () -> Any) :
        this(args.toList())
}

/**
 * Structure for storing and concatenating lines of elements.
 * @property values the individual values.
 * @property indices the indices of line breaks.
 */
data class Lines<T>(
    val values: List<T> = emptyList(),
    val indices: List<Int> = emptyList()
) {
    /**
     * Creates a new instance, with the added list of values and indices.
     * @param values the values.
     * @param indices the line indices.
     * @return the new lines.
     */
    fun add(values: List<T>, indices: List<Int>): Lines<T> {
        val size = this.values.size
        return Lines(
            this.values + values,
            this.indices + indices.map { it + size }
        )
    }

    /**
     * Creates a new instance, combining this and the specified lines.
     * @param o the other lines instance.
     * @return the new lines.
     */
    operator fun plus(o: Lines<T>) = add(o.values, o.indices)

    /**
     * Splits the values into a 2D list of [T] elements.
     * @return the 2D list.
     */
    fun lines(): List<List<T>> {
        val res = ArrayList<List<T>>()
        var lastIndex = 0
        indices.forEach { index ->
            res.add(values.subList(lastIndex, index))
            lastIndex = index
        }
        if (lastIndex < values.size) {
            res.add(values.subList(lastIndex, values.size))
        }
        return res
    }

    companion object {
        /**
         * Creates an instance from lines in text.
         * @param text the text.
         * @param mapper the mapper from line to [T].
         * @return the lines instance.
         */
        @JvmStatic
        fun <T> fromText(text: String, mapper: (String) -> T): Lines<T> {
            val lines = text.split('\n')
            val lineIndices = lines.indices.toMutableList()
            return Lines(lines.map(mapper), lineIndices.subList(1, lineIndices.size))
        }
    }
}

/** Token for the current scope element in a format. */
const val THIS = "_"
/** Key for elements which are separators of other tokens. */
const val SEPARATOR = "__separator__"

/**
 * An immutable parsed version of a message.
 * @property children the children of this node.
 */
sealed interface TemplateNode {
    val children: List<TemplateNode>

    /**
     * Appends another node to this node's children.
     * @param node the child to add.
     * @return the resulting node.
     */
    operator fun plus(node: TemplateNode): TemplateNode

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
fun TemplateNode.renderTree(): List<String> = Trees.render(children, { it.treeValue() }, { it.renderTree() })

data class ContainerNode(
    override val children: List<TemplateNode> = emptyList()
) : TemplateNode {
    override fun plus(node: TemplateNode) = ContainerNode(children + node)

    override fun treeValue() = listOf("<container>")
}

/**
 * A node holding raw text.
 * @property value the text.
 */
data class TextNode(
    val value: String,
    override val children: List<TemplateNode> = emptyList()
) : TemplateNode {
    override fun plus(node: TemplateNode) = TextNode(value, children + node)

    override fun treeValue() = value.split('\n').map { "<$it>" }

    override fun toString() = "\"$value\""
}

/**
 * A node defining a scope block enter.
 * @property label the label to enter the block with.
 * @property separator the text of the child separator.
 */
data class ScopeNode(
    val label: String,
    val separator: String,
    override val children: List<TemplateNode> = emptyList()
) : TemplateNode {
    override fun plus(node: TemplateNode) = ScopeNode(label, separator, children + node)

    override fun treeValue() = listOf("@$label [${separator.split("\n").joinToString(" | ")}]")

    override fun toString() = "@$label$children"
}

const val SCOPE_PATTERN = "@([a-z0-9_]+)\\["
const val ENTER = '['
const val EXIT = ']'

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
                if (isNotEmpty()) {
                    val idx = size - 1
                    val last = get(idx)
                    if (last is TextNode) {
                        set(idx, TextNode(last.value + value))
                    }
                } else {
                    add(TextNode(value))
                }
            }
        }

        val res = ArrayList<TemplateNode>()
        Regex(SCOPE_PATTERN).find(format, startIndex)?.let { start ->
            res.add(TextNode(format.substring(startIndex, start.range.first)))
            val label = start.groups[1]!!.value

            format.countEnterExit(ENTER, EXIT,
                { content, i ->
                    val (separator, idx) = if (i + 1 < format.length && format[i + 1] == ENTER)
                        format.countEnterExit(ENTER, EXIT,
                            { separator, i2 -> separator to i2 },
                            { "" to i }, // if we don't terminate separator properly, it's just more content
                            i + 2
                        )
                    else "" to i

                    res.add(ScopeNode(label, separator, parseChildren(content)))
                    res.addAll(parseChildren(format, idx + 1))
                },
                { content ->
                    // Lenient method (here): just add the remainder as text
                    // Strict method: throw an unbalanced brackets exception
                    res.addText(format.substring(start.range.first))
                },
                start.range.last + 1
            )
        } ?: run {
            res.addText(format.substring(startIndex))
        }
        return res
    }

    /**
     * Parses out a format string into its constituent nodes.
     *
     * Scope block enter/exits are done through [SCOPE_PATTERN] and [EXIT],
     * where the text immediately after the `@` defines the label,
     * the text between the first set of brackets defines the content,
     * and the text between the last set of brackets defines the separator.
     *
     * **Format**
     *
     *     Actions on {date, date, short}: @action[
     *       Purchases: {purchases, number} @entry[
     *         - {item} x{amount}]]
     *
     * **Result**
     *
     *     ├─ <Actions on {date, date, short}: >
     *     └─ @action []
     *         ├─ <>
     *         │  <  Purchases: {purchases, number} >
     *         └─ @entry []
     *             └─ <>
     *                <    - {item} x{amount}>
     *
     * @param format the string format.
     * @param startIndex the index in the string from which to start.
     * @return the resulting node.
     */
    fun parse(format: String, startIndex: Int = 0): TemplateNode {
        val res = parseChildren(format, startIndex)
        return if (res.size == 1) res[0] else ContainerNode(res)
    }

    /**
     * A single text part of a formatted result.
     * @property path the absolute position of this token in the tree.
     * @property value the text value.
     */
    data class FormatToken(val path: List<String>, val value: String)

    /**
     * Formats a node generated from [parseChildren] into lines of tokens.
     *
     * **Node**
     *
     *     ├─ <Actions on {date, date, long}: >
     *     └─ @action []
     *         ├─ <>
     *         │  <  {purchases, plural, one {# purchase:} other {# purchases:}} >
     *         └─ @entry []
     *             └─ <>
     *                <    - {item} x{amount}>
     *
     * **Arguments**
     * ```json
     * {
     *   "date": "01/01/1970",
     *   "action": [
     *     {
     *       "purchases": 5,
     *       "entry": [
     *         { "item": "Item One", "amount": 3 },
     *         { "item": "Item Two", "amount": 2 }
     *       ]
     *     },
     *     {
     *       "purchases": 1,
     *       "entry": [
     *         { "item": "Some Item", "amount": 1 }
     *       ]
     *     }
     *   ]
     * }
     * ```
     * **Result**
     *
     *     Actions on January 1, 1970:
     *       5 purchases:
     *         - Item One x3
     *         - Item Two x2
     *       1 purchase:
     *         - Some Item x1
     *
     * @param locale the locale to generate for, used in [MessageFormat.format].
     * @param node the node to generate with.
     * @param args the arguments.
     * @param path the current depth of the node.
     * @return the lines of tokens.
     */
    fun format(locale: Locale, node: TemplateNode, args: Args, path: List<String> = emptyList()): Lines<FormatToken> = when (node) {
        is TextNode -> {
            val builtArgs = HashMap<String, Any?>()
            args.forEach { key, value ->
                /*
                Other method: invoke the arg only if it appears as an ICU template
                But this is relatively hacky. Instead, we'll just invoke all args in the current scope.
                if (node.value.contains("{$key")) { // ICU template start
                    builtArgs[key] = value.invoke()
                }

                 */
                builtArgs[key] = value.invoke()
            }
            Lines.fromText(MessageFormat(node.value, locale).asString(builtArgs)) { FormatToken(path, it) }
        }
        is ScopeNode -> {
            fun linesOfArgs(args: Args): Lines<FormatToken> {
                var lines = Lines<FormatToken>()
                node.children.forEach { child ->
                    lines += format(locale, child, args, path + node.label)
                }
                return lines
            }

            args[node.label]?.let { valueFactory ->
                fun handle(value: Any): Lines<FormatToken> = when (value) {
                    is MultiArgs -> {
                        var totalLines = Lines<FormatToken>()
                        val childLines = value.args.map { handle(it()) }
                        val separator = Lines.fromText(node.separator) { FormatToken(path + node.label + SEPARATOR, it) }
                        childLines.forEachIndexed { idx, lines ->
                            totalLines += lines
                            if (idx < childLines.size - 1) {
                                totalLines += separator
                            }
                        }
                        totalLines
                    }
                    is Args -> linesOfArgs(value)
                    else -> linesOfArgs(Args(mapOf(THIS to { value })))
                }

                handle(valueFactory())
            } ?: Lines()
        }
        else -> {
            var lines = Lines<FormatToken>()
            node.children.forEach {
                lines += format(locale, it, args, path)
            }
            lines
        }
    }
}

private fun <T> CharSequence.countEnterExit(
    enter: Char,
    exit: Char,
    onExit: (String, Int) -> T,
    onRemainder: (String) -> T,
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
    return onRemainder(content.toString())
}
