package io.gitlab.aecsocket.glossa.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.flattener.FlattenerListener
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import java.util.*

private const val START = "\u001b["
private const val END = "m"
private const val RESET = "0"

/**
 * The color level that a terminal supports.
 */
interface ColorLevel {
    /**
     * Converts the provided RGB color into an escape sequence.
     */
    fun escape(rgb: Int): String

    companion object {
        /** TrueColor terminal level. */
        @JvmStatic
        val TrueColor = object : ColorLevel {
            override fun escape(rgb: Int) =
                "38;2;${rgb shr 16 and 0xff};${rgb shr 0 and 0xff};${rgb and 0xff}"
        }

        /** Terminals with 256 colors. */
        @JvmStatic
        val Indexed256 = object : ColorLevel {
            // todo
            override fun escape(rgb: Int) = Indexed16.escape(rgb)
        }

        /** Terminals with 16 colors. */
        @JvmStatic
        val Indexed16 = object : ColorLevel {
            override fun escape(rgb: Int) = when (rgb) {
                0x000000 -> "30"
                0x0000aa -> "34"
                0x00aa00 -> "32"
                0x00aaaa -> "36"
                0xaa0000 -> "31"
                0xaa00aa -> "35"
                0xffaa00 -> "33"
                0xaaaaaa -> "37"
                0x555555 -> "90"
                0x5555ff -> "94"
                0x55ff55 -> "92"
                0x55ffff -> "96"
                0xff5555 -> "91"
                0xff55ff -> "95"
                0xffff55 -> "93"
                0xffffff -> "97"
                else -> "39" // reset
            }
        }
    }
}

/**
 * Renders [Component]s into strings using ANSI escape sequences.
 *
 * Adapted from https://github.com/KyoriPowered/ansi/blob/trunk/src/main/java/net/kyori/ansi/ColorLevel.java
 * for Kotlin.
 */
object AnsiComponentRenderer {
    /**
     * Gets the color level of the current environment.
     */
    fun colorLevel(): ColorLevel {
        System.getenv("COLORTERM")?.let {
            if (it == "truecolor" || it == "24bit") return ColorLevel.TrueColor
        }
        System.getenv("TERM")?.let {
            if (it.contains("256color")) return ColorLevel.Indexed256
        }
        return ColorLevel.Indexed16
    }

    /**
     * Renders the passed component into a string with ANSI escape codes.
     */
    fun render(input: Component): String {
        val result = StringBuilder()
        val colorLevel = colorLevel()
        ComponentFlattener.basic().flatten(input, object : FlattenerListener {
            val styles = Stack<Style>()

            override fun pushStyle(style: Style) {
                styles.push(style)
            }

            override fun component(text: String) {
                val styleBuilder = Style.style()
                styles.forEach { styleBuilder.merge(it) }
                val style = styleBuilder.build()

                val joins = StringJoiner(";")
                joins.add(RESET)
                style.color()?.let { color ->
                    joins.add(colorLevel.escape(color.value()))
                }
                style.decorations().forEach { (dec, state) ->
                    if (state == TextDecoration.State.TRUE) {
                        joins.add(when (dec) {
                            TextDecoration.BOLD -> "1"
                            TextDecoration.ITALIC -> "3"
                            TextDecoration.UNDERLINED -> "4"
                            TextDecoration.OBFUSCATED -> "5"
                            TextDecoration.STRIKETHROUGH -> "9"
                            else -> throw NullPointerException()
                        })
                    }
                }
                result.append(START)
                    .append(joins)
                    .append(END)
                result.append(text)
            }

            override fun popStyle(style: Style) {
                styles.pop()
            }
        })
        return "$result$START$RESET$END"
    }

    /**
     * Renders the passed component-like into a string with ANSI escape codes.
     */
    fun render(like: ComponentLike) = render(like.asComponent())
}

/** @see [AnsiComponentRenderer.render] */
fun Component.ansi() = AnsiComponentRenderer.render(this)
/** @see [AnsiComponentRenderer.render] */
fun ComponentLike.ansi() = AnsiComponentRenderer.render(this)
