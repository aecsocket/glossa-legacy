package com.github.aecsocket.glossa.core

import org.junit.jupiter.api.Test
import java.util.*

class StringI18NTest {
    @Test
    fun test() {
        val locale = Locale.US
        val i18n = StringI18N(locale)

        val template = Template.parse("""
            Alpha: @alpha{_, number} | Beta: @beta{_, number}
        """.trimIndent())

        println(template)

        val args = Argument.buildMap(i18n, locale) {
            raw("alpha") { 5.5 }
            raw("beta") { 1_234.5 }
        }

        println(args)
        i18n.register(locale) {
            section("message") {
                value("one", "Alpha: @alpha{_, number} | Beta: @beta{_, number} | Sub: @sub( (sep.. @beta{_}) )")
                value("two", """
                    Actions: @actions[
                      @total{_, plural, one {# purchase} other {# purchases}} on @date{_, date, short}: @purchases[
                        - "@name()" x@amount{_, number}]]
                """.trimIndent())
            }
        }

        i18n.make(locale, "message", "one") {
            raw("alpha") { println("GEN ALPHA"); 5.5 }
            raw("beta") { println("GEN BETA"); 1_234.5 }
            sub("sub") { println("GEN SUB"); listOf("Hello", "World") }
        }?.let { lines ->
            println("Lines:")
            lines.forEach { println("  $it") }
        } ?: println("(no lines)")

        val date = Date(System.currentTimeMillis())

        data class Item(val id: String) : Localizable<String> {
            override fun localize(ctx: I18NContext<String>) = i18n.safe("item.$id")
        }

        i18n.make(locale, "message", "two") {
            list("actions") {
                map {
                    raw("total") { 5 }
                    raw("date") { date }
                    list("purchases") {
                        map {
                            tl("name") { Item("item_one") }
                            raw("amount") { 3 }
                        }
                        map {
                            tl("name") { Item("item_two") }
                            raw("amount") { 2 }
                        }
                    }
                }
                map {
                    raw("total") { 1 }
                    raw("date") { date }
                    list("purchases") {
                        map {
                            sub("name") { listOf("Item") }
                            raw("amount") { 1 }
                        }
                    }
                }
            }
        }?.forEach { println(it) }
    }
}
