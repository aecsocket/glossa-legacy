package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style.style
import org.junit.jupiter.api.Test
import java.util.Locale

class SimpleTest {
    @Test
    fun doTest() {
        val i18n = StylingI18N()
        i18n.register(Locale.ROOT,
            "basic" to """
                Authors: <@author(, ){_}@>
            """.trimIndent())
        i18n.styles["info"] = style(NamedTextColor.GRAY)
        i18n.styles["var"] = style(NamedTextColor.WHITE)
        i18n.styles["separator"] = style(NamedTextColor.YELLOW)
        i18n.formats["basic"] = StylingI18N.Format("info",
            listOf("author") to "separator",
            listOf("author", "__separator") to "var")

        println(i18n["basic", Args(
            "author" to {MultiArgs(
                { "AuthorOne" },
                { "AuthorTwo" }
            )}
        )].ansi("basic"))
    }
}

fun List<Component>?.ansi(key: String) = """[ $key:
${this?.joinToString("\n") { "  >${it.ansi()}<" } ?: "  (null lines)"}
]""".trimMargin()
