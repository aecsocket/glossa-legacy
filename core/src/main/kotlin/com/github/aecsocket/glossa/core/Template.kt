package com.github.aecsocket.glossa.core

sealed interface Template {
    val name: String

    data class Text(val value: String) : Template {
        override val name: String
            get() = "text"
        override fun toString() = "\"$value\""
    }

    data class Holder(val children: List<Template>) : Template {
        override val name: String
            get() = "holder"
        override fun toString() = children.toString()
    }

    data class Raw(
        val key: String,
        val content: String
    ) : Template {
        override val name: String
            get() = "raw"
        override fun toString() = "@$key{$content}"
    }

    data class Substitution(
        val key: String
    ) : Template {
        override val name: String
            get() = "substitution"
        override fun toString() = "@$key()"
    }

    data class Scope(
        val key: String,
        val content: List<Template>,
        val separator: List<Template>
    ) : Template {
        override val name: String
            get() = "scope"
        override fun toString() = "@$key[${content.joinToString(", ")}][${separator.joinToString(", ")}]"
    }

    class ParsingException(
        val index: Int,
        val rawMessage: String? = null,
        row: Int,
        col: Int,
        cause: Throwable? = null
    ) : I18NException("${row+1}:${col+1}${if (rawMessage == null) "" else " $rawMessage"}", cause) {
        fun combine(text: String, index: Int) = from(text, this.index + index, rawMessage, cause)

        companion object {
            fun from(text: String, index: Int, message: String? = null, cause: Throwable? = null): ParsingException {
                val (row, col) = text.rowCol(index)
                return ParsingException(index, message, row, col, cause)
            }
        }
    }

    companion object {
        private val TOKEN = "@(\\S+?)([{(\\[])".toRegex()
        private const val RAW_ENTER = '{'
        private const val RAW_EXIT = '}'
        private const val SUBSTITUTION_ENTER = '('
        private const val SUBSTITUTION_EXIT = ')'
        private const val SCOPE_ENTER = '['
        private const val SCOPE_EXIT = ']'

        private fun parseChildren(format: String, startIndex: Int = 0): List<Template> {
            val res = ArrayList<Template>()

            fun MutableList<Template>.addText(value: String) {
                if (value.isNotEmpty()) {
                    add(Text(value))
                }
            }

            fun MutableList<Template>.merge(other: List<Template>) {
                if (other.isEmpty())
                    return
                val last = last()
                val next = other.first()
                // merge text templates
                if (isNotEmpty() && last is Text && next is Text) {
                    set(size - 1, Text(last.value + next.value))
                    addAll(other.drop(1))
                } else {
                    addAll(other)
                }
            }

            TOKEN.find(format, startIndex)?.let { match ->
                val start = match.range.first
                val key = try {
                    Keys.validate(match.groups[1]!!.value)
                } catch (ex: KeyValidationException) {
                    throw ParsingException.from(format, start + ex.index, "Invalid key", ex)
                }

                res.addText(format.substring(startIndex, start))

                fun tryParse(content: String): List<Template> {
                    try {
                        return parseChildren(content)
                    } catch (ex: ParsingException) {
                        throw ex.combine(format, start)
                    }
                }

                val contentStart = match.range.last
                val nextStart = when (val type = match.groups[2]!!.value) {
                    RAW_ENTER.toString() -> format.countEnterExit(
                        RAW_ENTER, RAW_EXIT, contentStart,
                        { content, end ->
                            res.add(Raw(key, content))
                            end
                        },
                        { throw ParsingException.from(format, format.length, "Not enough raw exits") },

                    )
                    SUBSTITUTION_ENTER.toString() -> format.countEnterExit(
                        SUBSTITUTION_ENTER, SUBSTITUTION_EXIT, contentStart,
                        { _, end ->
                            res.add(Substitution(key))
                            end
                        },
                        { throw ParsingException.from(format, format.length, "Not enough substitution exits") }
                    )
                    SCOPE_ENTER.toString() -> format.countEnterExit(
                        SCOPE_ENTER, SCOPE_EXIT, contentStart,
                        { content, end ->
                            val (separator, lastEnd) = format.safe(end + 1)?.let {
                                if (it == SCOPE_ENTER) {
                                    format.countEnterExit(
                                        SCOPE_ENTER, SCOPE_EXIT, end + 1,
                                        { cSeparator, cEnd -> tryParse(cSeparator) to cEnd },
                                        { throw ParsingException.from(format, format.length, "Not enough separator exits") }
                                    )
                                } else null
                            } ?: (emptyList<Template>() to end)
                            res.add(Scope(key, tryParse(content), separator))
                            lastEnd
                        },
                        { throw ParsingException.from(format, format.length, "Not enough scope exits") }
                    )
                    else -> throw IllegalStateException("opening bracket $type")
                }

                res.addAll(parseChildren(format, nextStart + 1))
            } ?: res.addText(format.substring(startIndex))

            return res
        }

        fun parse(text: String, startIndex: Int = 0): Template {
            val res = parseChildren(text, startIndex)
            return if (res.size == 1) res[0]
                else Holder(res)
        }

        private fun <R> CharSequence.countEnterExit(
            enter: Char,
            exit: Char,
            startIndex: Int,
            onExit: (String, Int) -> R,
            onRemainder: (String) -> R
        ): R {
            var depth = 1
            val content = StringBuilder()
            for (i in startIndex + 1 until length) {
                when (val ch = get(i)) {
                    enter -> {
                        depth++
                        content.append(ch)
                    }
                    exit -> {
                        depth--
                        if (depth == 0) {
                            return onExit(content.toString(), i)
                        } else {
                            content.append(ch)
                        }
                    }
                    else -> content.append(ch)
                }
            }
            return onRemainder(content.toString())
        }
    }
}

private fun CharSequence.safe(index: Int) = if (index < length) get(index) else null

private data class RowCol(val row: Int, val col: Int)

private fun CharSequence.rowCol(index: Int): RowCol {
    var row = 0
    var col = 0
    for (i in 0 until index) {
        if (get(i) == '\n') {
            row++
            col = 0
        } else {
            col++
        }
    }
    return RowCol(row, col)
}
