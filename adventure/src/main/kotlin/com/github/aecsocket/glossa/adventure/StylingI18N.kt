package com.github.aecsocket.glossa.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import java.util.Locale

const val TEMPLATE_ENTER = "\${"
const val TEMPLATE_EXIT = "}"

class InvalidFormatException(key: String, row: Int, col: Int, message: String)
    : RuntimeException("($key: ${row+1}:${col+1}) $message")

class StylingI18N(
    locale: Locale = Locale.ROOT
) : AdventureI18N(locale) {
    data class Format(val default: String? = null, val args: Map<String, String> = emptyMap()) {
        companion object {
            @JvmStatic val IDENTITY = Format()
        }
    }

    private val styles = HashMap<String, Style>()
    private val formats = HashMap<String, Format>()

    override fun doGet(
        locale: Locale,
        key: String,
        args: Map<String, () -> List<Component>>
    ): List<Component>? {
        return translations[locale]?.get(key)?.let { tl ->
            val format = formats[key] ?: Format.IDENTITY

            /*
            You got <amount> x items
              (amount to "5") ->
              You got 5 x items

              (amount to [ "One", "Two" ]) ->
              You got One x items
              You got Two x items

             You gave <amount> to <user>
               (amount to "5", user to "Player") ->
               You gave 5 to Player
               (amount to ["5", "10"], user to "Player") ->
               You gave 5 to Player
               You gave 10 to Player
               (amount to ["5", "10"], user to ["Player1", "Player2"]) ->
               You gave 5 to Player1
               You gave 10 to Player1
               You gave 5 to Player2
               You gave 10 to Player2
             */

            val gotArgs = HashMap<String, List<Component>>()

            fun arg(key: String) = gotArgs.computeIfAbsent(key) {
                args[it]?.invoke() ?: throw IllegalArgumentException("Unknown arg key $key")
            }

            val allLines = ArrayList<Component>()
            tl.forEachIndexed { row, line ->
                data class Token(val arg: String, val pre: String, val values: List<Component>) {
                    override fun toString() = "\"$pre\" \${$arg}"
                }
                val tokens = ArrayList<Token>()
                var valuesSize = 0
                val postTokens: String

                run {
                    // the parser here is scoped, so we don't leak useless vars
                    var scanIdx = 0
                    var readIdx = 0
                    while (true) {
                        val enterIdx = line.indexOf(TEMPLATE_ENTER, scanIdx)
                        if (enterIdx == -1) {
                            // no more templates to tokenize
                            postTokens = line.substring(readIdx)
                            break
                        } else {
                            val exitIdx = line.indexOf(TEMPLATE_EXIT, enterIdx)
                            if (exitIdx == -1)
                                throw InvalidFormatException(key, row, enterIdx,
                                    "Unbalanced template enter/exit")
                            val arg = line.substring(enterIdx + TEMPLATE_ENTER.length, exitIdx)
                            if (args.containsKey(arg)) {
                                // is a valid arg
                                val values = arg(arg)
                                if (tokens.isNotEmpty()) {
                                    val last = tokens.last()
                                    if (last.values.size != values.size)
                                        throw InvalidFormatException(key, row, exitIdx,
                                            "All templates on one line must have same number of arguments " +
                                            "(arg '${last.arg}' had ${last.values.size}, arg '$arg' has ${values.size})")
                                }
                                valuesSize = values.size
                                tokens.add(Token(arg, line.substring(readIdx, enterIdx), values))
                            } else {
                                // not a valid arg - we set the parser to scan from the end of this,
                                // but the next will include this invalid placeholder + its pre
                                // (we don't change readIdx)
                                scanIdx = exitIdx + 1
                                continue
                            }
                            scanIdx = exitIdx + 1
                            readIdx = scanIdx
                        }
                    }
                }

                if (tokens.isNotEmpty()) {
                    (0 until valuesSize).forEach { idx ->
                        var addLine = text()
                        tokens.forEach { token ->
                            addLine += text(token.pre) + token.values[idx]
                        }
                        addLine += text(postTokens)
                        allLines += addLine.build()
                    }
                } else {
                    allLines += text(postTokens)
                }
            }

            allLines
        } ?: if (locale == this.locale) null else doGet(this.locale, key, args)
    }
}
