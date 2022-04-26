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

        assertEquals(listOf("Templated message: >1,234.6<"), i18n[TEMPLATED,
            "value" to { ctx -> listOf(ctx.format("%,.1f", 1_234.5678)) }])
        assertEquals(listOf("DE: Templated message: >1.234,6<"), i18n[Locale.GERMAN, TEMPLATED,
                "value" to { ctx -> listOf(ctx.format("%,.1f", 1_234.5678)) }])

        // TODO separators? : 1,234.568
        // TODO if we use DE and there's no value for DE, it falls back to US (fine)
        //   but then the locale used for formatting is US (not fine?)
        assertEquals(listOf("Templated message: >1234.568<"), i18n[TEMPLATED,
            "value" to { ctx -> listOf(ctx.format(1_234.5678)) }])
        assertEquals(listOf("DE: Templated message: >1234,568<"), i18n[Locale.GERMAN, TEMPLATED,
            "value" to { ctx -> listOf(ctx.format(1_234.5678)) }])
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
}
