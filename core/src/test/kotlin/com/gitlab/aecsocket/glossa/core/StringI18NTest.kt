package com.gitlab.aecsocket.glossa.core

import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.glossa.core.Localizable
import com.gitlab.aecsocket.glossa.core.StringI18NBuilder
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class StringI18NTest {
    private val root = Locale.ROOT
    private val english = Locale.US
    private val german = Locale.GERMAN
    private val french = Locale.FRENCH

    private fun i18n() = StringI18NBuilder(english).apply {
        register(english) {
            value("basic", "EN: Basic message")
            value("templated_string", "EN: String = @string{_}")
            value("templated_number", """
                EN: Number = @number{_, number}
                Decimal = @decimal{_, number, :: .00}
                Percent = @percent{_, number, percent}
            """.trimIndent())
            value("templated_date", """
                EN: Short = @date{_, date, short}
                Long = @date{_, date, long}
            """.trimIndent())

            value("repeated", """
                List: @list[
                  - @name{_}]
            """.trimIndent())
            value("repeated_compact", """
                List: @list[
                  - @_{_}]
            """.trimIndent())
            value("repeated_separated", "List: @list[@_{_}][, ]")
            value("substituted_single", "Sub: @sub()")
            value("substituted_list", "Sub: @sub[@_()][ | ]")
            value("item", """
                '@name{_}'
                Count: @count{_, number}
            """.trimIndent())
        }

        register(german) {
            value("basic", "DE: Basic message")
            value("templated_string", "DE: String = @string{_}")
            value("templated_number", """
                DE: Number = @number{_, number}
                Decimal = @decimal{_, number, :: .00}
                Percent = @percent{_, number, percent}
            """.trimIndent())
            value("templated_date", """
                DE: Short = @date{_, date, short}
                Long = @date{_, date, long}
            """.trimIndent())
        }

        register(root) {
            value("root_fallback", "ROOT: fallback")
        }
    }.build()

    @Test
    fun testTranslation() {
        i18n().apply {
            assertEquals(listOf(
                "EN: Basic message"
            ), make(english, "basic"))

            assertEquals(listOf(
                "DE: Basic message"
            ), make(german, "basic"))

            assertEquals(listOf(
                "EN: Basic message"
            ), make(french, "basic"))

            assertEquals(listOf(
                "ROOT: fallback"
            ), make(english, "root_fallback"))
        }
    }

    @Test
    fun testSafe() {
        i18n().apply {
            assertEquals(listOf(
                "EN: Basic message"
            ), safe(english, "basic"))

            assertEquals(listOf(
                "unknown_key"
            ), safe(english, "unknown_key"))
        }
    }

    @Test
    fun testRawStrings() {
        i18n().apply {
            assertEquals(listOf(
                "EN: String = test string"
            ), make(english, "templated_string") {
                raw("string") { "test string" }
            })

            assertEquals(listOf(
                "DE: String = test string"
            ), make(german, "templated_string") {
                raw("string") { "test string" }
            })
        }
    }

    @Test
    fun testRawNumbers() {
        i18n().apply {
            assertEquals(listOf(
                "EN: Number = 12,345",
                "Decimal = 34.57",
                "Percent = 35%"
            ), make(english, "templated_number") {
                raw("number") { 12_345 }
                raw("decimal") { 34.567 }
                raw("percent") { 0.35 }
            })

            assertEquals(listOf(
                "DE: Number = 12.345",
                "Decimal = 34,57",
                "Percent = 35 %" // nbsp here
            ), make(german, "templated_number") {
                raw("number") { 12_345 }
                raw("decimal") { 34.567 }
                raw("percent") { 0.35 }
            })
        }
    }

    @Test
    fun testRawDates() {
        val date = Date(0)
        i18n().apply {
            assertEquals(listOf(
                "EN: Short = 1/1/70",
                "Long = January 1, 1970"
            ), make(english, "templated_date") {
                raw("date") { date }
            })

            assertEquals(listOf(
                "DE: Short = 01.01.70",
                "Long = 1. Januar 1970"
            ), make(german, "templated_date") {
                raw("date") { date }
            })
        }
    }

    @Test
    fun testScopes() {
        i18n().apply {
            assertEquals(listOf(
                "List: ",
                "  - One",
                "  - Two"
            ), make(english, "repeated") {
                list("list") {
                    map {
                        raw("name") { "One" }
                    }
                    map {
                        raw("name") { "Two" }
                    }
                }
            })

            assertEquals(listOf(
                "List: ",
                "  - One",
                "  - Two"
            ), make(english, "repeated_compact") {
                list("list") {
                    raw("One")
                    raw("Two")
                }
            })
        }
    }

    @Test
    fun testScopesSeparated() {
        i18n().apply {
            assertEquals(listOf(
                "List: One, Two, Three"
            ), make(english, "repeated_separated") {
                list("list") {
                    raw("One")
                    raw("Two")
                    raw("Three")
                }
            })
        }
    }

    @Test
    fun testSubstitution() {
        i18n().apply {
            assertEquals(listOf(
                "Sub: text value"
            ), make(english, "substituted_single") {
                sub("sub") { "text value" }
            })

            assertEquals(listOf(
                "Sub: one | two"
            ), make(english, "substituted_list") {
                list("sub") {
                    sub("one")
                    sub("two")
                }
            })
        }
    }

    @Test
    fun testSubLocalized() {
        data class Item(val name: String, val count: Int) : Localizable<String> {
            override fun localize(i18n: I18N<String>) =
                i18n.safe("item") {
                    raw("name") { name }
                    raw("count") { count }
                }
        }

        val item = Item("item", 3)

        i18n().apply {
            assertEquals(listOf(
                "Sub: 'item' | Count: 3"
            ), make(english, "substituted_list") {
                subList("sub") { item.localize(this) }
            })

            assertEquals(listOf(
                "Sub: 'item' | Count: 3"
            ), make(english, "substituted_list") {
                tl("sub") { item }
            })
        }
    }
}
