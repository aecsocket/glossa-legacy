package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals

const val BASIC = "basic"
const val TEMPLATED = "templated"
const val MULTI_TEMPLATED = "multi_templated"
const val MULTI_LINE = "multi_line"
const val MULTI_VALUES = "multi_values"
const val PLURAL = "plural"
const val UNKNOWN_KEY = "unknown_key"
const val US_ONLY = "us_only"
const val UNBALANCED_ENTER_EXIT = "unbalanced_enter_exit"

class StringI18NTest {
    private fun i18n() = StringI18N(Locale.US).apply {
        register(Translation(Locale.US, mapOf(
            BASIC to listOf("Basic message"),
            TEMPLATED to listOf("Templated message: >\${value}<"),
            MULTI_TEMPLATED to listOf("Multi templated message: >\${one}< and >\${two}<"),
            MULTI_LINE to listOf("Top line", "Bottom line"),
            MULTI_VALUES to listOf("Actions:", "  Entry: \${action} at \${time}"),
            PLURAL to listOf("Purchases made on {date, date, ::dMMM}:",
                "  Purchased {<items>, plural, one {# item} other {# items}} at {<time>, time, ::jmm}"),
            US_ONLY to listOf("US only message"),
            UNBALANCED_ENTER_EXIT to listOf("Unbalanced enter/exit: \${bad_template"),
        )))
        register(Translation(Locale.UK, mapOf(
            BASIC to listOf("UK: Basic message"),
            TEMPLATED to listOf("UK: Templated message: >\${value}<"),
            MULTI_TEMPLATED to listOf("UK: Multi templated message: >\${one}< and >\${two}<"),
            MULTI_LINE to listOf("UK: Top line", "UK: Bottom line")
        )))
        register(Translation(Locale.GERMAN, mapOf(
            TEMPLATED to listOf("DE: Templated message: >\${value}<")
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
    }

    sealed interface MutableNode : Node {
        override val children: MutableList<Node>
    }

    data class TextNode(val value: String) : Node {
        override val children = emptyList<Node>()

        override fun toString() = "\"$value\""
    }

    data class RootNode(override val children: MutableList<Node> = mutableListOf()) : MutableNode {
        override fun toString() = children.toString()
    }

    data class ScopedNode(
        val label: String,
        val separator: String,
        override val children: MutableList<Node> = mutableListOf(),
    ) : MutableNode {
        override fun toString() = "@$label$children"
    }

    @Test
    fun todoRemove() {
        class ParsingException(row: Int, col: Int, message: String) : RuntimeException("${row+1}:${col+1}: $message") {
            constructor(string: String, idx: Int, message: String) :
                    // todo i dont like this
                this(string.rowColOf(idx).row, string.rowColOf(idx).col, message)
        }

        fun parse(format: String): RootNode {
            fun buildWalk(parent: MutableNode, sIdx: Int = 0, path: List<String> = emptyList()): Int {
                var idx = sIdx
                while (true) {
                    val enter = format.indexOf(SCOPE_ENTER, idx)
                    val exit = format.indexOf(SCOPE_EXIT, idx)
                    if (enter != -1 && (exit == -1 || enter < exit)) {
                        parent.children.add(TextNode(format.substring(idx, enter)))
                        val labelEnter = enter + SCOPE_ENTER.length

                        data class IndexGet(val labelExit: Int, val metaEnter: Int, val metaExit: Int)

                        val (labelExit, metaEnter, metaExit) = run {
                            for (cur in labelEnter until format.length) {
                                val ch = format[cur]
                                when {
                                    ch == SCOPE_SEPARATOR_ENTER -> {
                                        if (cur == idx)
                                            throw ParsingException(format, cur, "No label on scope enter")
                                        return@run IndexGet(cur,
                                            cur + 1,
                                            format.lastEnterExit(cur + 1, SCOPE_SEPARATOR_ENTER, SCOPE_SEPARATOR_EXIT)
                                                ?: throw ParsingException(format, cur,
                                                    "Unbalanced brackets in scope label definition"))
                                    }
                                    !LABEL_CHARS.contains(ch) -> throw ParsingException(format, cur,
                                        "Illegal character in label name: found '$ch', allowed [$LABEL_CHARS]")
                                }
                            }
                            throw ParsingException(format, labelEnter, "Unterminated label definition")
                        }
                        val label = format.substring(labelEnter, labelExit)
                        val separator = format.substring(metaEnter, metaExit)
                        val node = ScopedNode(label, separator).also { parent.children.add(it) }
                        idx = buildWalk(node, metaExit + 1, path + label) + SCOPE_EXIT.length
                    } else if (exit != -1 && (enter == -1 || exit < enter)) {
                        if (path.isEmpty())
                            throw ParsingException(format, exit, "Too many exits")
                        parent.children.add(TextNode(format.substring(idx, exit)))
                        return exit
                    } else if (path.isNotEmpty()) {
                        throw ParsingException(format, format.length, "Too many entrances")
                    } else {
                        parent.children.add(TextNode(format.substring(idx)))
                        return format.length
                    }
                }
            }

            return RootNode().apply {
                buildWalk(this)
            }
        }

        data class Token(val key: List<String>, val value: String)

        fun format(node: Node, args: Map<String, () -> Any>, path: List<String> = emptyList()): List<Token> = when (node) {
            is TextNode -> {
                val builtArgs = HashMap<String, Any?>()
                args.forEach { (key, value) ->
                    // todo parse each template individually cause tokenization (for component thing)
                    if (node.value.contains("{$key")) { // ICU template start
                        builtArgs[key] = value.invoke()
                    }
                }
                listOf(Token(path + BASE, MessageFormat(node.value, Locale.US).asString(builtArgs)))
            }
            is ScopedNode -> {
                fun addArg(arg: Map<String, () -> Any>) =
                    node.children.flatMap { format(it, arg, path + node.label) }

                args[node.label]?.let { arg ->
                    @Suppress("UNCHECKED_CAST") // cast and pray
                    when (val value = arg.invoke()) {
                        is Collection<*> -> {
                            value as Collection<Map<String, () -> Any>>
                            val mapped = value.map { addArg(it) }
                            val res = ArrayList<Token>()
                            val separator = Token(path + node.label + SEPARATOR, node.separator)
                            for (i in mapped.indices) {
                                res.addAll(mapped[i])
                                if (i < mapped.size - 1) {
                                    res.add(separator)
                                }
                            }
                            res
                        }
                        is Map<*, *> -> {
                            addArg(value as Map<String, () -> Any>)
                        }
                        else -> addArg(mapOf(THIS to { value }))
                    }
                } ?: emptyList() // todo handle this differently?
            }
            else -> node.children.flatMap { format(it, args) }
        }

        fun tokenize(node: Node, vararg args: Pair<String, () -> Any>) = format(node, args.associate { it })

        fun asString(tokens: List<Token>): String {
            val res = StringBuilder()
            tokens.forEach { res.append(it.value) }
            return res.toString()
        }

        val date = Date(System.currentTimeMillis())
        /*
        styles applied to: {
          __base__: "info"
          date: "var"
          transaction: {
            __base__: "var"
            __separator__: "extra"
            amount: "key"
            time: "key"
            entry: {
              __base__: "info"
              name: "var"
              amount: "var"
            }
          }
        }
         */
        println(asString(tokenize(parse("""
            Purchases on <@date(){_, date, ::dMMM}@>:<@transaction(
            
              ---
            )
              <@amount(){_, plural, one {# purchase} other {# purchases}}@> at <@time(){_, time, ::jmm}@>:<@entry()
                - <@name()"{_}"@> x<@amount(){_, number, integer}@>@>@>
        """.trimIndent()),
            "date" to { date },
            "transaction" to { listOf(mapOf(
                "amount" to { 5 },
                "time" to { date },
                "entry" to { listOf(mapOf(
                    "name" to { "Item One" },
                    "amount" to { 3 }
                ), mapOf(
                    "name" to { "Item Two" },
                    "amount" to { 2 }
                )) }
            ), mapOf(
                "amount" to { 1 },
                "time" to { date },
                "entry" to { listOf(mapOf(
                    "name" to { "Single Item" },
                    "amount" to { 1 }
                )) }
            )) })))

        val tokens = tokenize(parse("""
            Authors: <@author(, )<@name(){_}@> at <@email(){_}@>@>
        """.trimIndent()),
            "author" to { listOf(mapOf(
                "name" to { "AuthorOne" },
                "email" to { "one@email.com" }
            ), mapOf(
                "name" to { "AuthorTwo" },
                "email" to { "two@email.com" }
            )) })

        println(tokens.joinToString(",\n  ", "[\n  ", "\n]"))
        println(asString(tokens))
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
