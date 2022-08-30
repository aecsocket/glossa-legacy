package com.gitlab.aecsocket.glossa.core

import com.gitlab.aecsocket.glossa.adventure.MiniMessageI18N
import com.gitlab.aecsocket.glossa.adventure.ansi
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
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
                    value("no_permission", "You do not have permission '<permission>' number = <some_number> (<verbose>blah blah <some_number></verbose>)")
                }
            }

            style("info") { color(GRAY) }
            style("error") { color(RED) }
            style("var") { color(WHITE) }

            format {
                node("section") {
                    node("child") { style("info") }
                }
                node("error") {
                    style("error")
                    node("no_permission") {
                        argStyle("permission", "var")
                        argStyle("verbose", "info")
                        argTemplate("some_number", "{some_number, number}", "var")
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
            raw("permission", text("the.permission"))
            icu("some_number", 1234.56)
        }.print()
    }
}
