package com.gitlab.aecsocket.glossa.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class AdventureI18NTest {
    private val root = Locale.ROOT

    private fun i18n() = AdventureI18NBuilder(root).apply {
        register(root) {
            value("basic", "Basic")
            value("substitution", "Substitution: [@sub()]")
        }
    }.build()

    private fun assertEqualText(expected: List<Component>?, actual: List<Component>?) {
        val expectedSer = expected?.joinToString("\n", "[\n", "\n]") {
            "  " + GsonComponentSerializer.gson().serialize(it)
        } ?: "null"
        val actualSer = actual?.joinToString("\n", "[\n", "\n]") {
            "  " + GsonComponentSerializer.gson().serialize(it)
        } ?: "null"
        assertEquals(expectedSer, actualSer)
    }

    @Test
    fun testResult() {
        val i18n = i18n()
        assertEqualText(listOf(
            text("")
                .append(text("Basic"))
        ), i18n.make("basic"))

        assertEqualText(listOf(
            text("")
                .append(text("Substitution: ["))
                .append(text("Text"))
                .append(text("]"))
        ), i18n.make("substitution") {
            sub("sub") { text("Text") }
        })

        assertEqualText(listOf(
            text("")
                .append(text("Substitution: ["))
                .append(text("Text", RED))
                .append(text("]"))
        ), i18n.make("substitution") {
            sub("sub") { text("Text", RED) }
        })
    }
}
