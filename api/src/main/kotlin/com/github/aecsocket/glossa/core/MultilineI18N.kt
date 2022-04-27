package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.util.Locale

class MultilineI18NException(key: String, row: Int, message: String) :
    I18NException(key, "${row+1}: $message")

interface MultilineI18N<E, A> : I18N<List<E>, List<A>> {
    fun parse(
        locale: Locale,
        tl: List<String>,
        key: String,
        args: Map<String, (I18NContext<List<E>, List<A>>) -> List<A>>,
        rawAsE: (String) -> E,
        tokensAsE: (Int, List<Templater.Token<List<A>>>, String) -> E,
        st: (String) -> E
    ): List<E> {
        val lines = ArrayList<E>()
        val argCache = ArgCache(args)
        val ctx = asContext(locale)
        tl.forEachIndexed { row, line ->
            fun template(line: String, argFunction: (String) -> List<A>?): Templater.Result<List<A>> {
                val tokens = ArrayList<Templater.Token<List<A>>>()
                var scanIdx = 0
                var readIdx = 0
                while (true) {
                    val enterIdx = line.indexOf(TEMPLATE_ENTER, scanIdx)
                    if (enterIdx == -1) {
                        // no more templates to tokenize
                        return Templater.Result(tokens, line.substring(readIdx))
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
                            tokens.add(Templater.Token(argKey, line.substring(readIdx, enterIdx), argValue))
                            readIdx = exitIdx + 1
                        }
                        scanIdx = exitIdx + 1
                    }
                }
            }

            val (tokens, postTokens) = try {
                template(line) { argCache[ctx, it] }
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
                    val format = tokens.joinToString("") { token -> token.pre + token.key } + postTokens
                    val msg = MessageFormat(format, locale).asString(tokens.associate { token ->
                        token.key to token.value[idx]
                    })
                    lines += st(msg)
                    //lines += tokensAsE(idx, tokens, postTokens)
                }
            } else {
                lines += rawAsE(postTokens)
            }
        }

        return lines
    }
}
