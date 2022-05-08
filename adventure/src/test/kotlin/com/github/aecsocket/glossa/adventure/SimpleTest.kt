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
        i18n.formats["basic"] = StylingFormat("info",
            listOf("author") to "separator",
            listOf("author", "__separator") to "var")
    }
}
