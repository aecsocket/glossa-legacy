package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.I18N
import com.github.aecsocket.glossa.core.StringI18N
import com.github.aecsocket.glossa.core.Translation
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.junit.jupiter.api.Test
import java.util.Locale

class SimpleTest {
    @Test
    fun doTest() {
        val i18n = StylingI18N()
        i18n.register(Translation(Locale.ROOT, mapOf(
            "transactions" to listOf("Your transactions:", "  a: \${transaction} b: \${transactions}"),
            "transaction" to listOf("Tx of $\${amount} from \${user} received"),
            "il_transactions" to listOf("Your tx's:", "  \${transaction")
        )))

        println(i18n["transaction",
            "amount" to { listOf(text("15"), text(30)) },
            "user" to { listOf(text("Steve"), text("Alice")) }
        ].serialize("transaction"))

        println(i18n["transactions",
            "transactions" to { i18n.safe("transaction",
                "amount" to { listOf(text("15"), text("30")) },
                "user" to { listOf(text("Steve"), text("Alex")) }
            ) }
        ].serialize("transactions"))

        val stringI18N = StringI18N()
        stringI18N.register(Translation(Locale.ROOT, mapOf(
            "transactions" to listOf("Your transactions:", "  \${transactions}"),
            "transaction" to listOf("Tx of $\${amount} from \${user} received")
        )))

        stringI18N["transactions",
            "transactions" to { stringI18N.safe("transaction",
                "amount" to { listOf("15", "30") },
                "user" to { listOf("Steve", "Alex") }) }
        ]?.forEach { println(it) }

        println(text()
            .append(text("", NamedTextColor.YELLOW)
                .append(text("Well, "))
                .append(text("hello ", NamedTextColor.RED))
                .append(text("Steve", NamedTextColor.BLUE))
                .append(text("! You have ")
                .append(text("1 ", null, TextDecoration.BOLD))
                .append(text("new", NamedTextColor.RED, TextDecoration.UNDERLINED, TextDecoration.ITALIC))
                .append(text(" message.")))
            ).ansi())
    }
}

fun Component.serialize() = PlainTextComponentSerializer.plainText().serialize(this)
fun List<Component>?.serialize(key: String) = if (this == null) """[ $key
  (no lines created)
]""" else """[ key = $key
${joinToString("\n") { " >${it.serialize()}<" }}
]"""
