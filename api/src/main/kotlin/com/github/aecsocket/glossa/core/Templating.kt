package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.util.*
import kotlin.collections.HashMap

data class Args(val args: Map<String, () -> Any>) {
    constructor(vararg args: Pair<String, () -> Any>) :
            this(args.associate { it })

    operator fun get(index: String) = args[index]

    operator fun plus(o: Args) = Args(args + o.args)

    operator fun plus(arg: Pair<String, () -> Any>) = Args(args + arg)

    fun forEach(action: (String, () -> Any) -> Unit) = args.forEach(action)

    companion object {
        @JvmStatic val EMPTY = Args(emptyMap())
    }
}

data class MultiArgs(val args: Collection<() -> Any>) {
    constructor(vararg args: () -> Any) :
        this(args.toList())
}

class ParsingException(row: Int, col: Int, message: String) : RuntimeException("${row+1}:${col+1}: $message") {
    companion object {
        @JvmStatic
        fun from(text: String, idx: Int, message: String): ParsingException {
            val rowCol = text.rowColOf(idx)
            return ParsingException(rowCol.row, rowCol.col, message)
        }
    }
}

data class Lines<T>(
    val values: List<T> = emptyList(),
    val indices: List<Int> = emptyList()
) {
    fun add(values: List<T>, indices: List<Int>): Lines<T> {
        val size = this.values.size
        return Lines(
            this.values + values,
            this.indices + indices.map { it + size }
        )
    }

    operator fun plus(o: Lines<T>) = add(o.values, o.indices)

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
        @JvmStatic
        fun <T> fromText(text: String, mapper: (String) -> T): Lines<T> {
            val lines = text.split('\n')
            val lineIndices = lines.indices.toMutableList()
            return Lines(lines.map(mapper), lineIndices.subList(1, lineIndices.size))
        }
    }
}

const val SCOPE_ENTER = "<@"
const val SCOPE_SEPARATOR_ENTER = '('
const val SCOPE_SEPARATOR_EXIT = ')'
const val SCOPE_EXIT = "@>"

const val LABEL_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_"
const val THIS = "_"
const val BASE = "__base__"
const val SEPARATOR = "__separator__"

object Templating {
    sealed interface Node {
        val children: List<Node>

        fun treeValue(): List<String>
    }

    sealed interface MutableNode : Node {
        override val children: MutableList<Node>

        fun addText(value: String) {
            if (value.isNotEmpty()) {
                children.add(TextNode(value))
            }
        }
    }

    data class TextNode(val value: String) : Node {
        override val children = emptyList<Node>()

        override fun treeValue() = value.split('\n').map { "[$it]" }

        override fun toString() = "\"$value\""
    }

    data class HolderNode(override val children: MutableList<Node> = mutableListOf()) : MutableNode {
        override fun treeValue() = listOf("<holder>")

        override fun toString() = children.toString()
    }

    data class ScopedNode(
        val label: String,
        val separator: String,
        override val children: MutableList<Node> = mutableListOf(),
    ) : MutableNode {
        override fun treeValue() = listOf("@$label")

        override fun toString() = "@$label$children"
    }

    fun Node.renderTree(): List<String> = Trees.render(children, { it.treeValue() }, { it.renderTree() })

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

    data class FormatToken(val key: List<String>, val value: String)

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
            val newPath = path + BASE
            Lines.fromText(MessageFormat(node.value, locale).asString(builtArgs)) { FormatToken(newPath, it) }
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
