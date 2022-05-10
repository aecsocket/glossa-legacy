package com.github.aecsocket.glossa.core

import com.github.aecsocket.glossa.core.TemplatingI18N.Companion.arg
import com.github.aecsocket.glossa.core.TemplatingI18N.Companion.argList
import com.github.aecsocket.glossa.core.TemplatingI18N.Companion.argSub
import com.github.aecsocket.glossa.core.TemplatingI18N.Companion.args
import org.junit.jupiter.api.Test
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.BufferedReader
import java.io.StringReader
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals

const val SINGLE_LINE = "single_line"
const val MULTI_LINE = "multi_line"
const val TEMPLATED = "templated"
const val LOCALE_TEMPLATED = "locale_templated"
const val SCOPED = "scoped"
const val SCOPED_THIS = "scoped_this"
const val SEPARATED = "separated"
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
            SCOPED to "Scoped: >@scope[{value}]<",
            SCOPED_THIS to "Scoped this: >@value[{_}]<",
            SEPARATED to "Authors: @author[{name}][, ]",
            SEPARATORS_THIS to "Authors: @author[{_}][, ]",
            NESTED_SCOPE to """
                Purchases: @purchase[
                  {total, plural, one {# purchase} other {# purchases}}: @entry[
                    - "{name}" x{amount}]]
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
    fun test() {
        val i18n = StringI18N(Locale.US)
        i18n.register(Locale.US,
            "test" to """
                Test {value} subst @$<subst>[, ]
                Details: @<details>[
                  Number: {num, number}]
                Lines: @<line>[
                  <@$<line>>]
            """.trimIndent())
        i18n["test", args(
            "value" arg 5,
            "subst" argSub {listOf("one", "two")},
            "details" args {mapOf(
                "num" arg 12_345.6
            )},
            "line" argList {listOf(
                args("line" argSub {listOf("A")}),
                args("line" argSub {listOf("B")}),
            )}
        )]?.forEach { println("<$it>") }
    }

    /*@Test
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
        ), i18n[SEPARATED, Args(
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
            "Purchases: ",
            "  5 purchases: ",
            "    - \"Item one\" x3",
            "    - \"Item two\" x2",
            "  1 purchase: ",
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

    @Test
    fun testLoad() {
        val i18n = StringI18N(Locale.US)
        i18n.loadTranslations(HoconConfigurationLoader.builder()
            .source { BufferedReader(StringReader("""
            __locale__: "en-US"
            "message.single_line": "Single line"
            "message.multi_line": [
              "Line one"
              "Line two"
            ]
            """.trimIndent())) }
            .build())

        assertEquals(listOf(
            "Single line"
        ), i18n["message.single_line"])
        assertEquals(listOf(
            "Line one",
            "Line two"
        ), i18n["message.multi_line"])
    }

    @Test
    fun test() {
        val i18n = StringI18N(Locale.US)

        i18n["message", ]
    }*/
}
