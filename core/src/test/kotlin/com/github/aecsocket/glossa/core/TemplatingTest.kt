package com.github.aecsocket.glossa.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TemplatingTest {
    @Test
    fun testLines() {
        assertEquals(
            TextNode("Hi mom"),
            Templating.parse("Hi mom")
        )
        assertEquals(
            TextNode("""
                Line one
                Line two
            """.trimIndent()),
            Templating.parse("""
                Line one
                Line two
            """.trimIndent())
        )
    }

    @Test
    fun testScope() {
        assertEquals(
            RootNode(listOf(
                TextNode("Pre "),
                ScopeNode("label", "", listOf(
                    TextNode("Scope content")
                )),
                TextNode(" Post")
            )),
            Templating.parse("Pre @label[Scope content] Post")
        )

        assertEquals(
            RootNode(listOf(
                TextNode("Pre "),
                ScopeNode("one", "", listOf(
                    TextNode("Scope one "),
                    ScopeNode("two", "", listOf(
                        TextNode("Scope two")
                    ))
                )),
                TextNode(" Post")
            )),
            Templating.parse("Pre @one[Scope one @two[Scope two]] Post")
        )

        Templating.parse("Pre @one[Scope one @two[Mismatched brackets]")
            .renderTree().forEach { println(it) }

        assertEquals(
            TextNode("Pre @one[Scope one @two[Mismatched brackets]"),
            Templating.parse("Pre @one[Scope one @two[Mismatched brackets]")
        )
    }
}
