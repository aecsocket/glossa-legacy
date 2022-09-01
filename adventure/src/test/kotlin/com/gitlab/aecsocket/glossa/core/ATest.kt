package com.gitlab.aecsocket.glossa.core

import com.gitlab.aecsocket.glossa.adventure.MiniMessageI18N
import com.gitlab.aecsocket.glossa.adventure.ansi
import com.gitlab.aecsocket.glossa.adventure.load
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.ComponentSerializer
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import kotlin.test.Test

class ATest {
    @Test
    fun test() {
        val us = Locale.US
        val i18n = MiniMessageI18N.Builder().apply {
            translation(us) {
                value("one", "One")
                section("section") {
                    value("child", "Child")
                }
                section("error") {
                    value("no_permission",
                        "Reloaded with {entries, plural, =0 {no messages.} one {<entries>#</entries> message:} other {<entries>#</entries> messages:}}")
                }
            }

            style("info") { color(GRAY) }
            style("error") { color(RED) }
            style("var") { color(WHITE) }

            format {
                node("section") {
                    node("child") { style("info") }
                }
            }

            format {
                node("error") {
                    style("error")
                    node("no_permission") {
                        argStyle("entries", "var")
                    }
                }
            }
        }.build(us, MiniMessage.miniMessage())

        fun List<Component>.print() = forEach {
            println(it.ansi())
        }

        i18n.safe("section.abc").print()
        i18n.safe("section.child").print()
        i18n.safe("error.no_permission") {
            icu("entries", 4)
        }.print()

        val i18n2 = MiniMessageI18N.Builder().apply {
            load(HoconConfigurationLoader.builder().source { BufferedReader(StringReader("""
                styles: {
                  info: { color: "gray" }
                  var: { color: "white" }
                  error: { color: "red" }
                }
                formats: {
                  message: {
                    style: "info"
                    -: {
                      arg1: "var"
                      styled: "error"
                    }
                  }
                }
                en-US: {
                  message: "Hello world <arg1> and <styled>some styled</styled> text with ICU num <styled>{num, number}</styled>"
                }
            """.trimIndent())) }.build())
        }.build(Locale.US, MiniMessage.miniMessage())

        i18n2.safe("message") {
            subst("arg1", text("Arg1"))
            icu("num", 1234.56)
        }.print()
    }
}
