package com.github.aecsocket.glossa.core

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
const val SUBSTITUTED = "substituted"
const val SUBSTITUTED_SEPARATED = "substituted_separated"
const val LOCALIZABLE = "localizable"

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
            SCOPED to "Scoped: >@<scope>[{value}]<",
            SCOPED_THIS to "Scoped this: >@<value>[{_}]<",
            SEPARATED to "Authors: @<author>[{name}][, ]",
            SEPARATORS_THIS to "Authors: @<author>[{_}][, ]",
            NESTED_SCOPE to """
                Purchases: @<purchase>[
                  {total, plural, one {# purchase} other {# purchases}}: @<entry>[
                    - "{name}" x{amount}]]
            """.trimIndent(),
            SUBSTITUTED to "Substituted: >@$<subst><",
            SUBSTITUTED_SEPARATED to "Substituted: >@$<subst>[, ]<",
            LOCALIZABLE to "Localizable ({value})")
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
    fun testTemplated() {
        val i18n = i18n()
        assertEquals(listOf(
            "Template: >Template value<"
        ), i18n[TEMPLATED, argMap(
            "value" arg {"Template value"}
        )])
        assertEquals(listOf(
            "[UK] Template: >Template value<"
        ), i18n[Locale.UK, TEMPLATED, argMap(
            "value" arg {"Template value"}
        )])
    }

    @Test
    fun testLocaleTemplated() {
        val i18n = i18n()
        val date = Date(0)
        val args = argMap<String>(
            "num" arg {1_234.5},
            "prc" arg {0.2},
            "date" arg {date}
        )

        val pluralZero = args + ("items" arg {0})
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

        val pluralOne = args + ("items" arg {1})
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
        ), i18n[SCOPED, argMap(
            "scope" argMap {mapOf(
                "value" arg {
                    flag = true
                    "Value with side effect"
                }
            ) }
        )])
        assertEquals(true, flag)

        flag = false
        assertEquals(listOf(
            "Single line"
        ), i18n[SINGLE_LINE, argMap(
            "scope" argMap {mapOf(
                "value" arg {
                    flag = true
                    "Value with side effect"
                }
            ) }
        )])
        assertEquals(false, flag)

        assertEquals(listOf(
            "Scoped this: >This value<"
        ), i18n[SCOPED_THIS, argMap(
            "value" arg {"This value"}
        )])
    }

    @Test
    fun testSeparators() {
        val i18n = i18n()

        assertEquals(listOf(
            "Authors: AuthorOne, AuthorTwo, AuthorThree"
        ), i18n[SEPARATED, argMap(
            "author" argList {listOf(
                argMap("name" arg {"AuthorOne"}),
                argMap("name" arg {"AuthorTwo"}),
                argMap("name" arg {"AuthorThree"})
            )}
        )])

        assertEquals(listOf(
            "Authors: AuthorOne, AuthorTwo, AuthorThree"
        ), i18n[SEPARATORS_THIS, argMap(
            "author" argList {listOf(
                arg("AuthorOne"),
                arg("AuthorTwo"),
                arg("AuthorThree")
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
        ), i18n[NESTED_SCOPE, argMap(
            "purchase" argList {listOf(argMap(
                "total" arg {5},
                "entry" argList {listOf(argMap(
                    "name" arg {"Item one"},
                    "amount" arg {3}
                ), argMap(
                    "name" arg {"Item two"},
                    "amount" arg {2}
                ))}
            ), argMap(
                "total" arg {1},
                "entry" argList {listOf(argMap(
                    "name" arg {"Item"},
                    "amount" arg {1}
                ))}
            ))}
        )])
    }

    @Test
    fun testSubstitution() {
        val i18n = i18n()
        assertEquals(listOf(
            "Substituted: >Hello world<",
        ), i18n[SUBSTITUTED, argMap(
            "subst" argSub {listOf("Hello world")}
        )])

        assertEquals(listOf(
            "Substituted: >HelloWorld<", // if no separator defined, should be empty
        ), i18n[SUBSTITUTED, argMap(
            "subst" argSub {listOf("Hello", "World")}
        )])

        assertEquals(listOf(
            "Substituted: >Hello, World<",
        ), i18n[SUBSTITUTED_SEPARATED, argMap(
            "subst" argSub {listOf("Hello", "World")}
        )])

        data class OurLocalizable(val value: Int) : Localizable<String> {
            override fun localize(i18n: TemplatingI18N<String>, locale: Locale) =
                i18n.safe(locale, LOCALIZABLE, argMap(
                    "value" arg {value}
                ))
        }

        assertEquals(listOf(
            "Substituted: >Localizable (5)<"
        ), i18n[SUBSTITUTED, argMap(
            "subst" argTl {OurLocalizable(5)}
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
}
