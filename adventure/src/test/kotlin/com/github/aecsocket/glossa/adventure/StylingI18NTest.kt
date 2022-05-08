package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.Style.style
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.junit.jupiter.api.Test
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

const val INFO = "info"
const val VAR = "var"
const val EXTRA = "extra"

const val SINGLE_LINE = "single_line"
const val MULTI_LINE = "multi_line"
const val TEMPLATED = "templated"
const val SEPARATED = "separated"

class StylingI18NTest {
    private fun i18n() = StylingI18N(Locale.US).apply {
        register(Locale.US,
            SINGLE_LINE to "Single line",
            MULTI_LINE to """
                Line one
                Line two
            """.trimIndent(),
            TEMPLATED to "Template: >{value}<",
            SEPARATED to "Authors: @author[{_}][, ]")

        styles[INFO] = style(WHITE)
        styles[VAR] = style(YELLOW)
        styles[EXTRA] = style(GRAY)

        formats[TEMPLATED] = StylingFormat(INFO,
            "value" to VAR)
        formats[SEPARATED] = StylingFormat(INFO,
            "author" to VAR,
            "author.$SEPARATOR" to EXTRA)
    }

    private fun equalComponents(expected: List<Component>?, actual: List<Component>?) {
        if (expected == null || actual == null)
            assertNull(actual)
        else {
            assertEquals("[\n" +
                expected.joinToString("\n") { "  ${GsonComponentSerializer.gson().serialize(it)}" }
                + "\n]", "[\n" +
                actual.joinToString("\n") { "  ${GsonComponentSerializer.gson().serialize(it)}" }
                + "\n]")
        }
    }

    @Test
    fun testLines() {
        val i18n = i18n()

        equalComponents(listOf(
            text("Single line")
        ), i18n[SINGLE_LINE])
        equalComponents(listOf(
            text("Line one"),
            text("Line two")
        ), i18n[MULTI_LINE])
    }

    @Test
    fun testStyling() {
        val i18n = i18n()

        equalComponents(listOf(
            text("", WHITE) +
                text("Authors: ") +
                text("AuthorOne", YELLOW) +
                text(", ", GRAY) +
                text("AuthorTwo", YELLOW)
        ), i18n[SEPARATED, Args(
            "author" to {MultiArgs(
                {"AuthorOne"},
                {"AuthorTwo"}
            )}
        )])
    }

    @Test
    fun testLoad() {
        val i18n = StylingI18N(Locale.US)
        i18n.loadTranslations(HoconConfigurationLoader.builder()
            .source { BufferedReader(StringReader("""
            __locale__: "en-US"
            "message.single_line": "Single line"
            "message.multi_line": [
              "Line one"
              "Line two"
            ]
            "message.templated": [
              "Entry: @one[one = {value}]"
              "Entry: @two[two = {value}]"
            ]
            "message.authors": "Authors: @author[{_}][, ]"
            """.trimIndent())) }
            .build())

        i18n.loadStyling(HoconConfigurationLoader.builder()
            .source { BufferedReader(StringReader("""
            styles: {
              info: { color: "white" }
              var: { color: "yellow" }
              extra: { color: "gray" }
            }
            formats: {
              "message.templated": {
                $DEFAULT: "info"
                "one": "var"
                "two": "var"
                "two.value": "var"
              }
              "message.authors": {
                $DEFAULT: "info"
                "author": "var"
                "author.$SEPARATOR": "extra"
              }
            }
            """.trimIndent())) }
            .build())

        equalComponents(listOf(
            text("", WHITE) +
                text("Entry: ") +
                text("one = 1", YELLOW),
            text("", WHITE) +
                text("Entry: ") +
                text("two = 2", YELLOW)
        ), i18n["message.templated", Args(
            "one" to {Args(
                "value" to {1}
            )},
            "two" to {Args(
                "value" to {2}
            )}
        )])

        equalComponents(listOf(
            text("", WHITE) +
                text("Authors: ") +
                text("AuthorOne", YELLOW) +
                text(", ", GRAY) +
                text("AuthorTwo", YELLOW)
        ), i18n["message.authors", Args(
            "author" to {MultiArgs(
                {"AuthorOne"},
                {"AuthorTwo"}
            )}
        )])
    }
}

fun List<Component>?.ansi() = if (this == null)
    listOf("(null lines)")
else
    listOf("[") +
    map { "  <${it.ansi()}>" } +
    listOf("]")
