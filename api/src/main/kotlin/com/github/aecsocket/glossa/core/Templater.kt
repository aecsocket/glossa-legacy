package com.github.aecsocket.glossa.core

const val TEMPLATE_ENTER = "\${"
const val TEMPLATE_EXIT = "}"

class TemplatingException(val col: Int, override val message: String)
    : RuntimeException("$col: message")

object Templater {
    data class Result<T>(val tokens: List<Token<T>>, val post: String)

    data class Token<T>(val key: String, val pre: String, val value: T) {
        override fun toString() = "\"$pre\" ${TEMPLATE_ENTER}$key${TEMPLATE_EXIT}"
    }

    fun <T> template(line: String, argFunction: (String) -> T?): Result<T> {
        val tokens = ArrayList<Token<T>>()
        var scanIdx = 0
        var readIdx = 0
        while (true) {
            val enterIdx = line.indexOf(TEMPLATE_ENTER, scanIdx)
            if (enterIdx == -1) {
                // no more templates to tokenize
                return Result(tokens, line.substring(readIdx))
            } else {
                val exitIdx = line.indexOf(TEMPLATE_EXIT, enterIdx)
                if (exitIdx == -1)
                    throw TemplatingException(enterIdx, "Unbalanced template enter/exit")
                val argKey = line.substring(enterIdx + TEMPLATE_ENTER.length, exitIdx)

                val argValue = argFunction(argKey)
                if (argValue == null) {
                    // not a valid arg - we set the parser to scan from the end of this,
                    // but the next will include this invalid placeholder + its pre
                    // (we don't change readIdx)
                } else {
                    tokens.add(Token(argKey, line.substring(readIdx, enterIdx), argValue))
                    readIdx = exitIdx + 1
                }
                scanIdx = exitIdx + 1
            }
        }
    }
}
