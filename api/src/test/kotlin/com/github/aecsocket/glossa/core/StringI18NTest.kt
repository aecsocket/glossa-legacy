package com.github.aecsocket.glossa.core

import com.github.aecsocket.glossa.core.Templating.renderTree
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals

const val SINGLE_LINE = "single_line"
const val MULTI_LINE = "multi_line"
const val TEMPLATED = "templated"
const val LOCALE_TEMPLATED = "locale_templated"
const val SCOPED = "scoped"
const val SCOPED_THIS = "scoped_this"
const val SEPARATORS = "separators"
const val SEPARATORS_THIS = "separators_this"
const val NESTED_SCOPE = "nested_scope"

class StringI18NTest {
    private fun i18n() = StringI18N(Locale.US).apply {
        register(Locale.US,
            SINGLE_LINE to "Single line",
            MULTI_LINE to """
                Line one
                Line two
            """.trimIndent(),
            TEMPLATED to "Template: >{value}<",
            LOCALE_TEMPLATED to """
                Number: {num, number}
                Percent: {prc, number, percent}
                Short date: {date, date, short}
                Long date: {date, date, long}
                Plural: {items, plural,
                  one {You have # item.}
                  other {You have # items.}
                }
            """.trimIndent(),
            SCOPED to "Scoped: ><@scope(){value}@><",
            SCOPED_THIS to "Scoped this: ><@value(){_}@><",
            SEPARATORS to "Authors: <@author(, ){name}@>",
            SEPARATORS_THIS to "Authors: <@author(, ){_}@>",
            NESTED_SCOPE to """
                Purchases:<@purchase()
                  {total, plural, one {# purchase} other {# purchases}}:<@entry()
                    - "{name}" x{amount}@>@>
            """.trimIndent())
        register(Locale.UK,
            SINGLE_LINE to "[UK] Single line",
            MULTI_LINE to """
                [UK] Line one
                [UK] Line two
            """.trimIndent(),
            TEMPLATED to "[UK] Template: >{value}<")
        register(Locale.GERMAN,
            LOCALE_TEMPLATED to """
                Number: {num, number}
                Percent: {prc, number, percent}
                Short date: {date, date, short}
                Long date: {date, date, long}
                Plural: {items, plural,
                  one {You have # item.}
                  other {You have # items.}
                }
            """.trimIndent())
    }

    @Test
    fun testSingleLine() {
        val i18n = i18n()

        Templating.parse("""
            Actions on {date, date, short}:<@action()
              Purchases: {purchases, number}<@entry()
                - {item} x{amount}@>@>
        """.trimIndent()).renderTree().forEach { println(it) }

        assertEquals(listOf(
            "Single line"
        ), i18n[SINGLE_LINE])
        assertEquals(listOf(
            "[UK] Single line"
        ), i18n[Locale.UK, SINGLE_LINE])
    }

    @Test
    fun testMultiLine() {
        val i18n = i18n()
        assertEquals(listOf(
            "Line one",
            "Line two"
        ), i18n[MULTI_LINE])
        assertEquals(listOf(
            "[UK] Line one",
            "[UK] Line two"
        ), i18n[Locale.UK, MULTI_LINE])
    }

    @Test
    fun testTemplated() {
        val i18n = i18n()
        assertEquals(listOf(
            "Template: >Template value<"
        ), i18n[TEMPLATED, Args(
            "value" to { "Template value" }
        )])
        assertEquals(listOf(
            "[UK] Template: >Template value<"
        ), i18n[Locale.UK, TEMPLATED, Args(
            "value" to { "Template value" }
        )])
    }

    @Test
    fun testLocaleTemplated() {
        val i18n = i18n()
        val date = Date(0)
        val args = Args(
            "num" to { 1_234.5 },
            "prc" to { 0.2 },
            "date" to { date }
        )

        val pluralZero = args + ("items" to { 0 })
        assertEquals(listOf(
            "Number: 1,234.5",
            "Percent: 20%",
            "Short date: 1/1/70",
            "Long date: January 1, 1970",
            "Plural: You have 0 items."
        ), i18n[LOCALE_TEMPLATED, pluralZero])
        assertEquals(listOf(
            "Number: 1.234,5",
            "Percent: 20 %",
            "Short date: 01.01.70",
            "Long date: 1. Januar 1970",
            "Plural: You have 0 items."
        ), i18n[Locale.GERMAN, LOCALE_TEMPLATED, pluralZero])

        val pluralOne = args + ("items" to { 1 })
        assertEquals(listOf(
            "Number: 1,234.5",
            "Percent: 20%",
            "Short date: 1/1/70",
            "Long date: January 1, 1970",
            "Plural: You have 1 item."
        ), i18n[LOCALE_TEMPLATED, pluralOne])
        assertEquals(listOf(
            "Number: 1.234,5",
            "Percent: 20 %",
            "Short date: 01.01.70",
            "Long date: 1. Januar 1970",
            "Plural: You have 1 item."
        ), i18n[Locale.GERMAN, LOCALE_TEMPLATED, pluralOne])
    }

    @Test
    fun testScope() {
        val i18n = i18n()

        var flag = false
        assertEquals(listOf(
            "Scoped: >Value with side effect<"
        ), i18n[SCOPED, Args(
            "scope" to { Args(
                "value" to {
                    flag = true
                    "Value with side effect"
                }
            ) }
        )])
        assertEquals(true, flag)

        flag = false
        assertEquals(listOf(
            "Single line"
        ), i18n[SINGLE_LINE, Args(
            "scope" to { Args(
                "value" to {
                    flag = true
                    "Value with side effect"
                }
            ) }
        )])
        assertEquals(false, flag)

        assertEquals(listOf(
            "Scoped this: >This value<"
        ), i18n[SCOPED_THIS, Args(
            "value" to { "This value" }
        )])
    }

    @Test
    fun testSeparators() {
        val i18n = i18n()

        assertEquals(listOf(
            "Authors: AuthorOne, AuthorTwo, AuthorThree"
        ), i18n[SEPARATORS, Args(
            "author" to { MultiArgs({ Args(
                "name" to { "AuthorOne" }
            ) }, { Args(
                "name" to { "AuthorTwo" }
            ) }, { Args(
                "name" to { "AuthorThree" }
            ) }) }
        )])

        assertEquals(listOf(
            "Authors: AuthorOne, AuthorTwo, AuthorThree"
        ), i18n[SEPARATORS_THIS, Args(
            "author" to { MultiArgs(
                { "AuthorOne" },
                { "AuthorTwo" },
                { "AuthorThree" }
            ) }
        )])
    }

    @Test
    fun testNestedScope() {
        val i18n = i18n()

        assertEquals(listOf(
            "Purchases:",
            "  5 purchases:",
            "    - \"Item one\" x3",
            "    - \"Item two\" x2",
            "  1 purchase:",
            "    - \"Item\" x1"
        ), i18n[NESTED_SCOPE, Args(
            "purchase" to { MultiArgs({ Args(
                "total" to { 5 },
                "entry" to { MultiArgs({ Args(
                    "name" to { "Item one" },
                    "amount" to { 3 }
                ) }, { Args(
                    "name" to { "Item two" },
                    "amount" to { 2 }
                ) }) }
            ) }, { Args(
                "total" to { 1 },
                "entry" to { MultiArgs({ Args(
                    "name" to { "Item" },
                    "amount" to { 1 }
                ) }) }
            ) }) }
        )])
    }
}
    /*private fun i18n() = StringI18N(Locale.US).apply {
        register(Translation(Locale.US, mapOf(
            BASIC to "Basic message",
            TEMPLATED to "Templated message: >\${value}<",
            MULTI_TEMPLATED to "Multi templated message: >\${one}< and >\${two}<",
            MULTI_LINE to """"""Top line", "Bottom line",
            MULTI_VALUES to "Actions:", "  Entry: \${action} at \${time}",
            PLURAL to """
                Purchases made on {date, date, ::dMMM}:
                  Purchased {<items>, plural, one {# item} other {# items}} at {<time>, time, ::jmm}"
          """.trimIndent(),
            US_ONLY to "US only message",
            UNBALANCED_ENTER_EXIT to "Unbalanced enter/exit: \${bad_template",
        )))
        register(Translation(Locale.UK, mapOf(
            BASIC to "UK: Basic message",
            TEMPLATED to "UK: Templated message: >\${value}<",
            MULTI_TEMPLATED to "UK: Multi templated message: >\${one}< and >\${two}<",
            MULTI_LINE to """
                UK: Top line
                UK: Bottom line
            """.trimIndent()
        )))
        register(Translation(Locale.GERMAN, mapOf(
            TEMPLATED to "DE: Templated message: >\${value}<"
        )))
    }

    @Test
    fun testBasic() {
        val i18n = i18n()
        assertEquals(listOf("Basic message"), i18n[BASIC])
        assertEquals(listOf("UK: Basic message"), i18n[Locale.UK, BASIC])
    }

    @Test
    fun testMultiLine() {
        val i18n = i18n()
        assertEquals(listOf("Top line", "Bottom line"), i18n[MULTI_LINE])
        assertEquals(listOf("UK: Top line", "UK: Bottom line"), i18n[Locale.UK, MULTI_LINE])
    }

    @Test
    fun testTemplate() {
        val i18n = i18n()
        assertEquals(listOf("Templated message: >Value<"), i18n[TEMPLATED,
            "value" to { listOf("Value") }])
        assertEquals(listOf("UK: Templated message: >Value<"), i18n[Locale.UK, TEMPLATED,
            "value" to { listOf("Value") }])

        assertEquals(listOf("Multi templated message: >Alpha< and >Beta<"), i18n[MULTI_TEMPLATED,
            "one" to { listOf("Alpha") },
            "two" to { listOf("Beta") }])
        assertEquals(listOf("UK: Multi templated message: >Alpha< and >Beta<"), i18n[Locale.UK, MULTI_TEMPLATED,
            "one" to { listOf("Alpha") },
            "two" to { listOf("Beta") }])

        assertEquals(listOf("Templated message: >\${value}<"), i18n[TEMPLATED])
        assertEquals(listOf("UK: Templated message: >\${value}<"), i18n[Locale.UK, TEMPLATED])
    }

    @Test
    fun testContext() {
        val i18n = i18n()
        assertEquals(listOf("Templated message: >Basic message<"), i18n[TEMPLATED,
            "value" to { ctx -> ctx.safe(BASIC) }])
        assertEquals(listOf("UK: Templated message: >UK: Basic message<"), i18n[Locale.UK, TEMPLATED,
            "value" to { ctx -> ctx.safe(BASIC) }])

        /*assertEquals(listOf("Templated message: >1,234.6<"), i18n[TEMPLATED,
            "value" to { ctx -> listOf(ctx.format("%,.1f", 1_234.5678)) }])
        assertEquals(listOf("DE: Templated message: >1.234,6<"), i18n[Locale.GERMAN, TEMPLATED,
            "value" to { ctx -> listOf(ctx.format("%,.1f", 1_234.5678)) }])

        // TODO separators? : 1,234.568
        // TODO if we use DE and there's no value for DE, it falls back to US (fine)
        //   but then the locale used for formatting is US (not fine?)
        assertEquals(listOf("Templated message: >1234.568<"), i18n[TEMPLATED,
            "value" to { ctx -> listOf(ctx.format(1_234.5678)) }])
        assertEquals(listOf("DE: Templated message: >1234,568<"), i18n[Locale.GERMAN, TEMPLATED,
            "value" to { ctx -> listOf(ctx.format(1_234.5678)) }])*/
    }

    @Test
    fun testMultiValues() {
        val i18n = i18n()
        assertEquals(listOf(
            "Actions:",
            "  Entry: File deleted at 12:05",
            "  Entry: File created at 12:06"), i18n[MULTI_VALUES,
            "action" to { listOf("File deleted", "File created") },
            "time" to { listOf("12:05", "12:06") }])
        assertEquals(listOf(
            "Actions:"), i18n[MULTI_VALUES,
                "action" to { emptyList() },
                "time" to { emptyList() }])
    }

    @Test
    fun testUnknownKey() {
        val i18n = i18n()
        assertEquals(null, i18n[UNKNOWN_KEY])
        assertEquals(null, i18n[Locale.UK, UNKNOWN_KEY])

        assertEquals(listOf(UNKNOWN_KEY), i18n.safe(UNKNOWN_KEY))
        assertEquals(listOf(UNKNOWN_KEY), i18n.safe(Locale.UK, UNKNOWN_KEY))
    }

    @Test
    fun testFallback() {
        val i18n = i18n()
        assertEquals(listOf("US only message"), i18n[US_ONLY])
        assertEquals(listOf("US only message"), i18n[Locale.UK, US_ONLY])
    }

    @Test
    fun testParsing() {
        val i18n = i18n()
        assertThrows<I18NException> { i18n[UNBALANCED_ENTER_EXIT] }
        assertThrows<I18NException> { i18n[MULTI_TEMPLATED,
            "one" to { listOf("A", "B") },
            "two" to { listOf("A") }] }
    }

    @Test
    fun testSideEffects() {
        val i18n = i18n()

        var flag = false
        assertEquals(listOf("Templated message: >Value<"), i18n[TEMPLATED,
            "value" to {
                flag = true
                listOf("Value")
            }])
        assertEquals(true, flag)

        flag = false
        assertEquals(listOf("Basic message"), i18n[BASIC,
            "value" to {
                flag = true
                listOf("Value")
            }])
        assertEquals(false, flag)

        flag = false
        assertEquals(listOf("Templated message: >\${value}<"), i18n[TEMPLATED,
            "not_value" to {
                flag = true
                listOf("Value")
            }])
        assertEquals(false, flag)
    }

    sealed interface Node {
        val children: List<Node>

        fun treeValue(): List<String>
    }

    fun Node.renderTree(): List<String> = Trees.render(children, { it.treeValue() }, { it.renderTree() })

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

    data class Args(val args: Map<String, () -> Any>) {
        constructor(vararg args: Pair<String, () -> Any>) :
            this(args.associate { it })

        operator fun get(index: String) = args[index]

        fun forEach(action: (String, () -> Any) -> Unit) = args.forEach(action)
    }

    data class MultiArgs(val args: Collection<Args>) {
        constructor(vararg args: Args) :
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

    @Test
    fun todoRemove() {
        fun parse(format: String): HolderNode {
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

        data class Token(val key: List<String>, val value: String)

        fun tokenize(node: Node, args: Args, path: List<String> = emptyList()): Lines<Token> = when (node) {
            is TextNode -> {
                val builtArgs = HashMap<String, Any?>()
                args.forEach { key, value ->
                    if (node.value.contains("{$key")) { // ICU template start
                        builtArgs[key] = value.invoke()
                    }
                }
                val newPath = path + BASE
                Lines.fromText(MessageFormat(node.value, Locale.US /* todo */).asString(builtArgs)) { Token(newPath, it) }
            }
            is ScopedNode -> {
                fun linesOfArgs(args: Args): Lines<Token> {
                    var lines = Lines<Token>()
                    node.children.forEach { child ->
                        lines += tokenize(child, args, path + node.label)
                    }
                    return lines
                }

                args[node.label]?.let { valueFactory ->
                    when (val value = valueFactory()) {
                        is MultiArgs -> {
                            var totalLines = Lines<Token>()
                            val childLines = value.args.map { linesOfArgs(it) }
                            val separator = Lines.fromText(node.separator) { Token(path + node.label + SEPARATOR, it) }
                            childLines.forEachIndexed { idx, lines ->
                                totalLines += lines
                                if (idx < childLines.size - 1) {
                                    totalLines += separator
                                }
                            }
                            return@let totalLines
                        }
                        is Args -> linesOfArgs(value)
                        else -> linesOfArgs(Args(mapOf(THIS to { value })))
                    }
                } ?: Lines()
            }
            else -> {
                var lines = Lines<Token>()
                node.children.forEach {
                    lines += tokenize(it, args, path)
                }
                lines
            }
        }

        fun asString(tokens: List<Token>): String {
            val res = StringBuilder()
            tokens.forEach { res.append("[${it.value}]") }
            return res.toString()
        }

        val date = Date(System.currentTimeMillis())
        val node = parse("""
            Purchases on <@date(){_, date, ::dMMM}@>:<@transaction(
            
              ---
            )
              <@amount(){_, plural, one {# purchase} other {# purchases}}@> at <@time(){_, time, ::jmm}@>:<@entry()
                - <@name()"{_}"@> x<@amount(){_, number, integer}@>@>@>
        """.trimIndent())

        node.renderTree().forEach { println(it) }

        val lines = tokenize(node, Args(
            "date" to { date },
            "transaction" to { MultiArgs(Args(
                "amount" to { 5 },
                "time" to { date },
                "entry" to { MultiArgs(Args(
                    "name" to { "Item One" },
                    "amount" to { 3 }
                ), Args(
                    "name" to { "Item Two" },
                    "amount" to { 2 }
                )) }
            ), Args(
                "amount" to { 1 },
                "time" to { date },
                "entry" to { MultiArgs(Args(
                    "name" to { "Single Item" },
                    "amount" to { 1 }
                )) }
            )) }))

        lines.lines().forEach { line ->
            println(asString(line))
        }

        val node2 = parse("""
            Authors: <@author(, )<@name(){_}@> at <@email(){_}@>@>
            ...
        """.trimIndent())

        println("\n")

        node2.renderTree().forEach { println(it) }

        val tokens = tokenize(node2, Args(
            "author" to { MultiArgs(Args(
                "name" to { "AuthorOne" },
                "email" to { "one@email.com" }
            ), Args(
                "name" to { "AuthorTwo" },
                "email" to { "two@email.com" }
            )) }))

        tokens.lines().forEach { line ->
            println(asString(line))
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

private data class RowCol(val row: Int, val col: Int)

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
*/