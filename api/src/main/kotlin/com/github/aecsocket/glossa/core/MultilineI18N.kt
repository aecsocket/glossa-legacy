package com.github.aecsocket.glossa.core

import java.util.Locale

class MultilineI18NException(key: String, val row: Int, message: String) :
    I18NException(key, "${row+1}: $message")

interface MultilineI18N<E> : I18N<List<E>> {
    companion object {
        fun <E> parse(
            tl: List<String>,
            key: String,
            args: Map<String, () -> List<E>>,
            rawAsE: (String) -> E,
            tokensAsE: (Int, List<Templater.Token<List<E>>>, String) -> E
        ): List<E> {
            val lines = ArrayList<E>()
            val argCache = ArgCache(args)
            tl.forEachIndexed { row, line ->
                val (tokens, postTokens) = try {
                    Templater.template(line) { argCache[key, it] }
                } catch (ex: TemplatingException) {
                    throw MultilineI18NException(key, row, ex.message)
                }

                var numValues = -1
                tokens.forEach { token ->
                    if (numValues == -1) {
                        numValues = token.value.size
                    } else if (numValues != token.value.size) {
                        throw MultilineI18NException(key, row,
                            "All templates on one line must have same number of arguments " +
                            "(previous args had $numValues arg(s), arg '${token.key}' has ${token.value.size})")
                    }
                }

                if (tokens.isNotEmpty()) {
                    (0 until numValues).forEach { idx ->
                        lines += tokensAsE(idx, tokens, postTokens)
                    }
                } else {
                    lines += rawAsE(postTokens)
                }
            }

            return lines
        }
    }
}
