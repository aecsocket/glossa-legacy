package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.Style.style
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.junit.jupiter.api.Test
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
            listOf("value") to VAR)
        formats[SEPARATED] = StylingFormat(INFO,
            listOf("author") to VAR,
            listOf("author", SEPARATOR) to EXTRA)
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
}

fun List<Component>?.ansi() = if (this == null)
    listOf("(null lines)")
else
    listOf("[") +
    map { "  <${it.ansi()}>" } +
    listOf("]")
