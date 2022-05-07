package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.util.*
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
 * An exception that occurs when parsing a string into a node.
 * @param row the row the exception occurs on.
 * @param col the column the exception occurs on.
 * @param message the exception message.
 */
class ParsingException(row: Int, col: Int, message: String) : RuntimeException("${row+1}:${col+1}: $message") {
    companion object {
        /**
         * Creates an exception by getting row/col from [rowColOf].
         * @param text the text to find row/col in.
         * @param idx the index.
         * @param message the exception message.
         */
        @JvmStatic
        fun from(text: String, idx: Int, message: String): ParsingException {
            val rowCol = text.rowColOf(idx)
            return ParsingException(rowCol.row, rowCol.col, message)
        }
    }
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

/** Token for entering a scope block. */
const val SCOPE_ENTER = "<@"
/** Token for entering a scope separator. */
const val SCOPE_SEPARATOR_ENTER = '('
/** Token for exiting a scope separator. */
const val SCOPE_SEPARATOR_EXIT = ')'
/** Token for exiting a scope block. */
const val SCOPE_EXIT = "@>"

/** Characters that are allowed to be used in a label. */
const val LABEL_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_"
/** Token for the current scope element in a format. */
const val THIS = "_"
/** Key for elements which are separators of other tokens. */
const val SEPARATOR = "__separator"

/**
 * Helper class for templating string messages using a custom scope
 * block format, combined with Unicode ICU [MessageFormat]s.
 *
 * See [parse], [format] for parsing and formatting detail.
 */
object Templating {
    /**
     * A parsed version of a message.
     * @property children the children of this node.
     */
    sealed interface Node {
        val children: List<Node>

        /**
         * Gets the value of this node in a rendered tree.
         * @return the lines of the value.
         */
        fun treeValue(): List<String>
    }

    /**
     * A mutable version of a node.
     */
    sealed interface MutableNode : Node {
        override val children: MutableList<Node>

        /**
         * Appends a [TextNode] to this node's children.
         *
         * If the value is empty, no node is added.
         * @param value The text contents.
         */
        fun addText(value: String) {
            if (value.isNotEmpty()) {
                children.add(TextNode(value))
            }
        }
    }

    /**
     * A node holding raw text.
     * @property value the text.
     */
    data class TextNode(val value: String) : Node {
        override val children = emptyList<Node>()

        override fun treeValue() = value.split('\n').map { "[$it]" }

        override fun toString() = "\"$value\""
    }

    /**
     * A node holding only child nodes.
     */
    data class HolderNode(override val children: MutableList<Node> = mutableListOf()) : MutableNode {
        override fun treeValue() = listOf("<holder>")

        override fun toString() = children.toString()
    }

    /**
     * A node defining a scope block enter.
     * @property label the label to enter the block with.
     * @property separator the text of the child separator.
     */
    data class ScopedNode(
        val label: String,
        val separator: String,
        override val children: MutableList<Node> = mutableListOf(),
    ) : MutableNode {
        override fun treeValue() = listOf("@$label")

        override fun toString() = "@$label$children"
    }

    /**
     * Renders out this node using [Trees.render].
     * @return the lines of the render.
     */
    fun Node.renderTree(): List<String> = Trees.render(children, { it.treeValue() }, { it.renderTree() })

    /**
     * Parses out a format string into its constituent nodes.
     *
     * Scope block enter/exits are done through [SCOPE_ENTER] and [SCOPE_EXIT],
     * where the text immediately after [SCOPE_ENTER] defines the label,
     * and the text between [SCOPE_SEPARATOR_ENTER] and [SCOPE_SEPARATOR_EXIT]
     * defines the separator.
     *
     * **Format**
     *
     *     Actions on {date, date, short}:<@action()
     *       Purchases: {purchases, number}<@entry()
     *         - {item} x{amount}@>@>
     *
     * **Result**
     *
     *     ├─ [Actions on {date, date, short}:]
     *     └─ @action
     *         ├─ []
     *         │  [  Purchases: {purchases, number}]
     *         └─ @entry
     *             └─ []
     *                [    - {item} x{amount}]
     *
     * @param format the string format.
     * @return the root node of the tree.
     * @throws ParsingException if the format could not be parsed.
     */
    fun parse(format: String): Node {
        fun buildWalk(parent: MutableNode, sIdx: Int = 0, path: List<String> = emptyList()): Int {
            var idx = sIdx
            while (true) {
                val enter = format.indexOf(SCOPE_ENTER, idx)
                val exit = format.indexOf(SCOPE_EXIT, idx)
                if (enter != -1 && (exit == -1 || enter < exit)) {
                    parent.addText(format.substring(idx, enter))
                    val labelEnter = enter + SCOPE_ENTER.length

                    data class IndexGet(val labelExit: Int, val metaEnter: Int, val metaExit: Int)

                    val (labelExit, metaEnter, metaExit) = run {
                        for (cur in labelEnter until format.length) {
                            val ch = format[cur]
                            when {
                                ch == SCOPE_SEPARATOR_ENTER -> {
                                    if (cur == idx)
                                        throw ParsingException.from(format, cur, "No label on scope enter")
                                    return@run IndexGet(cur,
                                        cur + 1,
                                        format.lastEnterExit(cur + 1, SCOPE_SEPARATOR_ENTER, SCOPE_SEPARATOR_EXIT)
                                            ?: throw ParsingException.from(format, cur,
                                                "Unbalanced brackets in scope label definition"))
                                }
                                !LABEL_CHARS.contains(ch) -> throw ParsingException.from(format, cur,
                                    "Illegal character in label name: found '$ch', allowed [$LABEL_CHARS]")
                            }
                        }
                        throw ParsingException.from(format, labelEnter, "Unterminated label definition")
                    }
                    val label = format.substring(labelEnter, labelExit)
                    val separator = format.substring(metaEnter, metaExit)
                    val node = ScopedNode(label, separator).also { parent.children.add(it) }
                    idx = buildWalk(node, metaExit + 1, path + label) + SCOPE_EXIT.length
                } else if (exit != -1 && (enter == -1 || exit < enter)) {
                    if (path.isEmpty())
                        throw ParsingException.from(format, exit, "Too many exits")
                    parent.addText(format.substring(idx, exit))
                    return exit
                } else if (path.isNotEmpty()) {
                    throw ParsingException.from(format, format.length, "Too many entrances")
                } else {
                    parent.addText(format.substring(idx))
                    return format.length
                }
            }
        }

        return HolderNode().apply {
            buildWalk(this)
        }
    }

    /**
     * A single text part of a formatted result.
     * @property path the absolute position of this token in the tree.
     * @property value the text value.
     */
    data class FormatToken(val path: List<String>, val value: String)

    /**
     * Formats a node generated from [parse] into lines of tokens.
     *
     * **Node**
     *
     *     ├─ [Actions on {date, date, long}:]
     *     └─ @action
     *         ├─ []
     *         │  [  {purchases, plural, one {# purchase:} other {# purchases:}}]
     *         └─ @entry
     *             └─ []
     *                [    - {item} x{amount}]
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
    fun format(locale: Locale, node: Node, args: Args, path: List<String> = emptyList()): Lines<FormatToken> = when (node) {
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
        is ScopedNode -> {
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

private data class RowCol(val row: Int, val col: Int)

private fun CharSequence.rowColOf(idx: Int): RowCol {
    var row = 0
    var col = 0
    for (i in 0 until idx) {
        if (get(i) == '\n') {
            row++
            col = 0
        } else {
            col++
        }
    }
    return RowCol(row, col)
}

private fun CharSequence.lastEnterExit(idx: Int, enter: Char, exit: Char): Int? {
    var depth = 1
    for (cur in idx until length) {
        when (get(cur)) {
            enter -> depth++
            exit -> {
                depth--
                if (depth <= 0)
                    return cur
            }
        }
    }
    return null
}
