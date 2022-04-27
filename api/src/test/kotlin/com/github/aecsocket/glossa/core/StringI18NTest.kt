package com.github.aecsocket.glossa.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        val children: MutableList<Node>
    }

    data class TextNode(val value: String) : Node {
        override val children = mutableListOf<Node>()

        override fun toString() = "\"$value\""
    }

    data class RootNode(override val children: MutableList<Node> = mutableListOf()) : Node {
        override fun toString() = children.toString()
    }

    data class ScopedNode(val label: String, override val children: MutableList<Node> = mutableListOf()) : Node {
        override fun toString() = "@$label${children}"
    }

    @Test
    fun todoRemove() {
        /*val i18n = i18n()
        val buf = StringBuffer()
        println("Purchases:")
        val fmt = "  You made {purchases, plural, one {# purchase} other {# purchases}} on {date, date, ::dMMM}"
        println(MessageFormat(fmt, Locale.US).format("purchases" to 4, "date" to Date(System.currentTimeMillis())))
        println(MessageFormat(fmt, Locale.US).format("purchases" to 1, "date" to Date(System.currentTimeMillis())))

        i18n[PLURAL,
            "date" to { listOf(Date(System.currentTimeMillis())) },
            "items" to { listOf(4, 1) },
            "time" to { listOf(Date(System.currentTimeMillis()), Date(System.currentTimeMillis())) }]
        ?.forEach { println(it) } ?: println("null")

        /*val format = listOf(
            "Purchases on {date, date, ::dMMM}:",
            "  {<group.amount>, plural, one {# purchase} other {# purchases}} at {<group.time>, time, ::jmm}",
            "    - Item '{<group.item.name>}' x{<group.item.amount>, number, integer}",
            "End of purchases")*/


        val date = Date(System.currentTimeMillis())
        i18n[PURCHASES,
            "date" to { date },
            "group" to { listOf(mapOf(
                "amount" to { 5 }, "time" to { date }, "item" to { listOf(
                    mapOf("name" to { "Game One" }, "amount" to { 1 }),
                    mapOf("name" to { "Game Two" }, "amount" to { 4 })) }
            ), mapOf(
                "amount" to { 1 }, "time" to { date }, "item" to { listOf(
                    mapOf("name" to { "Table" }, "amount" to { 1 })) }
            )) }]

        val out = """
            Purchases on 27 Apr:
              5 purchases at 11:03
                - Item 'Game One' x1
                - Item 'Game Two' x4
              1 purchase at 11:03
                - Item 'Table' x1
            End of purchases
        """.trimIndent()*/

        val format = """
            Purchases on {date, date, ::dMMM}:<@transaction
              {amount, plural, one {# purchase} other {# purchases}} at {time, time, ::jmm}<@entry
                - '{name}' x{amount, number, integer}@>@>
        """.trimIndent()

        class ParsingException(row: Int, col: Int, message: String) : RuntimeException("${row+1}:${col+1}: $message") {
            constructor(string: String, idx: Int, message: String) :
                    // todo i dont like this
                this(string.rowColOf(idx).row, string.rowColOf(idx).col, message)
        }

        fun foo(a: Int, b: Int) {}

        fun i18n(format: String, args: Map<String, () -> Any>): List<String> {
            val SCOPE_ENTER = "<@"
            val SCOPE_EXIT = "@>"

            // step 1. build node tree
            fun walk(sIdx: Int, parent: Node, depth: Int): Int {
                var idx = sIdx
                while (true) {
                    val enter = format.indexOf(SCOPE_ENTER, idx)
                    val exit = format.indexOf(SCOPE_EXIT, idx)
                    if (enter != -1 && (exit == -1 || enter < exit)) {
                        parent.children.add(TextNode(format.substring(idx, enter)))
                        val labelEnter = enter + SCOPE_ENTER.length
                        format.nextNonLabel(labelEnter)?.let { labelExit ->
                            val label = format.substring(labelEnter, labelExit)
                            val node = ScopedNode(label).also { parent.children.add(it) }
                            idx = walk(labelExit + 1, node, depth + 1) + SCOPE_EXIT.length
                        } ?: throw ParsingException(format, enter, "No label on scope enter")
                    } else if (exit != -1 && (enter == -1 || exit < enter)) {
                        if (depth == 0)
                            throw ParsingException(format, exit, "Too many exits")
                        parent.children.add(TextNode(format.substring(idx, exit)))
                        return exit
                    } else if (depth > 0) {
                        throw ParsingException(format, format.length, "Too many entrances")
                    } else {
                        parent.children.add(TextNode(format.substring(idx)))
                        return format.length
                    }
                }
            }

            val root = RootNode()
            walk(0, root, 0)

            println("nodes = $root")

            return emptyList()
        }

        fun i18n(format: String, vararg args: Pair<String, () -> Any>) = i18n(format, args.associate { it })

        i18n("hello <@label <@another_label @> <@yet_another_label with content@>@> and after")
    }
}

private fun CharSequence.nextNonLabel(idx: Int): Int? {
    for (cur in idx until length) {
        if (get(cur).isWhitespace()) {
            // if it ends the same place it starts, we return null
            // just so we don't have to handle it up there ^^
            return if (cur == idx) null else cur
        }
    }
    return null
}

private data class RowCol(val row: Int, val col: Int)

private fun CharSequence.rowColOf(idx: Int): RowCol {
    var row = 0
    var col = 0
    for (i in indices) {
        if (get(i) == '\n') {
            row++
            col = 0
        } else {
            col++
        }
    }
    return RowCol(row, col)
}
