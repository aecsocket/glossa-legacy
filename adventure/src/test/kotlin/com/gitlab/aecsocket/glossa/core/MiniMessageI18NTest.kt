package com.gitlab.aecsocket.glossa.core

import com.gitlab.aecsocket.glossa.adventure.MiniMessageI18N
import com.gitlab.aecsocket.glossa.adventure.ansi
import com.gitlab.aecsocket.glossa.adventure.load
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.BufferedReader
import java.io.StringReader
import java.util.Date
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MiniMessageI18NTest {
    val us = Locale.US
    val de = Locale.GERMAN
    val serializer = GsonComponentSerializer.gson()

    fun i18n() = MiniMessageI18N.Builder().apply {
        style("gray") { color(GRAY) }
        style("red") { color(RED) }
        style("blue") { color(BLUE) }

        format {
            node("formatted") {
                node("single") { style("gray") }
                node("section") { style("gray") }
                node("with_placeholders") {
                    argStyle("subst_arg", "red")
                    argStyle("icu_arg", "blue")
                }
            }
        }

        translation(us) {
            value("basic", "Basic message")
            value("multiline", "Line one", "Line two")
            section("section") {
                value("child", "Child of section")
            }
            value("locale_message", "US message")
            value("with_subst_arg", "Substituted argument: <subst_arg>")
            value("with_icu_num", "ICU number: {icu_num, number}")
            value("with_icu_date_short", "ICU date short: {icu_date, date, short}")
            value("with_icu_date_long", "ICU date long: {icu_date, date, long}")
            value("minimessage", "Unstyled <red>Red text <blue>Blue text <bold>Bold blue</bold> Unbold")
            value("minimessage_subst", "<red>[ <subst_arg> ]</red>")
            section("formatted") {
                value("single", "Single formatted")
                section("section") {
                    value("child", "Formatted under section")
                }
                value("with_placeholders", "Red subst arg: <subst_arg> | Blue ICU arg: <icu_arg>{icu_arg}</icu_arg>")
            }
            value("store_item", "[ID {id} cost {cost, number}]")
        }

        translation(de) {
            value("locale_message", "DE message")
        }
    }.build(us, MiniMessage.miniMessage())

    fun List<Component>.print() = forEach {
        println(it.ansi())
    }

    fun assertSame(expected: List<Component>, actual: List<Component>?) {
        assertNotNull(actual)
        try {
            assertEquals(
                expected.map { serializer.serialize(it) },
                actual.map { serializer.serialize(it) }
            )
        } catch (ex: Throwable) {
            println("-- Expected:")
            expected.print()
            println("-- Actual:")
            actual.print()
            throw ex
        }
    }

    @Test
    fun message() {
        val i18n = MiniMessageI18N.Builder().apply {
            style("red") { color(RED) }
            style("blue") { color(BLUE) }

            format {
                node("with_placeholders") {
                    argStyle("subst_arg", "red")
                    argStyle("icu_arg", "blue")
                }
            }

            translation(us) {
                value("list_separator", ", ")
                value("with_placeholders", "Red subst arg: <subst_arg> | Blue ICU arg: <icu_arg>{icu_arg}</icu_arg>")
                value("a_list", "List: <list>")
            }
        }.build(us, MiniMessage.miniMessage())

        i18n.safe("with_placeholders") {
            subst("subst_arg", text("Subst"))
            icu("icu_arg", "ICU")
        }.print()

        i18n.safe("a_list") {
            list("list", text("One", RED), text("Two", BLUE))
        }.print()
    }

    @Test
    fun testMessages() {
        val i18n = i18n()

        assertSame(listOf(
            text("Basic message")
        ), i18n.make("basic"))

        assertSame(listOf(
            text("Line one"),
            text("Line two")
        ), i18n.make("multiline"))

        assertSame(listOf(
            text("Child of section")
        ), i18n.make("section.child"))
    }

    @Test
    fun testSafe() {
        val i18n = i18n()

        assertSame(listOf(
            text("Basic message")
        ), i18n.safe("basic"))

        assertSame(listOf(
            text("unknown_key")
        ), i18n.safe("unknown_key"))
    }

    @Test
    fun testLocale() {
        val i18n = i18n()

        assertSame(listOf(
            text("US message")
        ), i18n.withLocale(us).make("locale_message"))

        assertSame(listOf(
            text("DE message")
        ), i18n.withLocale(de).make("locale_message"))
    }

    @Test
    fun testSubstitution() {
        val i18n = i18n()

        assertSame(listOf(
            text("Substituted argument: Value")
        ), i18n.make("with_subst_arg") {
            subst("subst_arg", text("Value"))
        })
    }

    @Test
    fun testIcuNumber() {
        val i18n = i18n()

        assertSame(listOf(
            text("ICU number: 1,234.56")
        ), i18n.withLocale(us).make("with_icu_num") {
            icu("icu_num", 1234.56)
        })

        assertSame(listOf(
            text("ICU number: 1.234,56")
        ), i18n.withLocale(de).make("with_icu_num") {
            icu("icu_num", 1234.56)
        })
    }

    @Test
    fun testIcuDateShort() {
        val i18n = i18n()
        val date = Date(0)

        assertSame(listOf(
            text("ICU date short: 1/1/70")
        ), i18n.withLocale(us).make("with_icu_date_short") {
            icu("icu_date", date)
        })

        assertSame(listOf(
            text("ICU date short: 01.01.70")
        ), i18n.withLocale(de).make("with_icu_date_short") {
            icu("icu_date", 1234.56)
        })
    }

    @Test
    fun testIcuDateLong() {
        val i18n = i18n()
        val date = Date(0)

        assertSame(listOf(
            text("ICU date long: January 1, 1970")
        ), i18n.withLocale(us).make("with_icu_date_long") {
            icu("icu_date", date)
        })

        assertSame(listOf(
            text("ICU date long: 1. Januar 1970")
        ), i18n.withLocale(de).make("with_icu_date_long") {
            icu("icu_date", 1234.56)
        })
    }

    @Test
    fun testMiniMessage() {
        val i18n = i18n()

        assertSame(listOf(
            text("Unstyled ")
                .append(text("Red text ", RED)
                    .append(text("Blue text ", BLUE)
                        .append(text("Bold blue", null, TextDecoration.BOLD))
                        .append(text(" Unbold"))))
        ), i18n.make("minimessage"))

        assertSame(listOf(
            text("[ Value ]", RED)
        ), i18n.make("minimessage_subst") {
            subst("subst_arg", text("Value"))
        })

        assertSame(listOf(
            text("[ ", RED)
                .append(text("Value", BLUE))
                .append(text(" ]"))
        ), i18n.make("minimessage_subst") {
            subst("subst_arg", text("Value", BLUE))
        })
    }

    @Test
    fun testFormatted() {
        val i18n = i18n()

        assertSame(listOf(
            text("Single formatted", GRAY)
        ), i18n.make("formatted.single"))

        assertSame(listOf(
            text("Formatted under section", GRAY)
        ), i18n.make("formatted.section.child"))

        assertSame(listOf(
            text("Red subst arg: ")
                .append(text("Subst", RED))
                .append(text(" | Blue ICU arg: "))
                .append(text("ICU", BLUE))
        ), i18n.make("formatted.with_placeholders") {
            subst("subst_arg", text("Subst"))
            icu("icu_arg", "ICU")
        })
    }

    @Test
    fun testLocalizable() {
        val i18n = i18n()

        data class StoreItem(
            val id: Int,
            val cost: Int
        ) : Localizable<Component> {
            override fun localize(i18n: I18N<Component>) = i18n.safeOne("store_item") {
                icu("id", id)
                icu("cost", cost)
            }
        }

        val item1 = StoreItem(1, 100)
        val item2 = StoreItem(2, 200)

        assertSame(listOf(
            text("Substituted argument: [ID 1 cost 100]")
        ), i18n.make("with_subst_arg") {
            subst("subst_arg", item1)
        })

        assertSame(listOf(
            text("Substituted argument: [ID 2 cost 200]")
        ), i18n.make("with_subst_arg") {
            subst("subst_arg", item2)
        })
    }

    @Test
    fun testConfigurate() {
        val config = """
            styles: {
              info: { color: "gray" }
              var: { color: "white" }
              error: { color: "red" }
            }
            formats: {
              "message": "info"
              "placeholders": [ "info", {
                subst_arg: "var"
              }]
              "error": "error"
              "error.no_permission": {
                permission: "var"
              }
            }
            en-US: {
              message: "Basic message"
              placeholders: "Message with substitution [ <subst_arg> ]"
              error: {
                no_permission: "You do not have permission node <permission>!"
              }
            }
        """.trimIndent()

        val i18n = MiniMessageI18N.Builder().apply {
            load(HoconConfigurationLoader.builder()
                .source { BufferedReader(StringReader(config)) }
                .build())
        }.build(us, MiniMessage.miniMessage())

        assertSame(listOf(
            text("Basic message", GRAY)
        ), i18n.make("message"))

        assertSame(listOf(
            text("Message with substitution [ ", GRAY)
                .append(text("Subst arg", WHITE))
                .append(text(" ]"))
        ), i18n.make("placeholders") {
            subst("subst_arg", text("Subst arg"))
        })

        assertSame(listOf(
            text("You do not have permission node ", RED)
                .append(text("some.permission", WHITE))
                .append(text("!"))
        ), i18n.make("error.no_permission") {
            subst("permission", text("some.permission"))
        })
    }
}
