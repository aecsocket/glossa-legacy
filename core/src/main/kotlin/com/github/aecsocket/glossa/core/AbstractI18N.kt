package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.text.FieldPosition
import java.util.*
import kotlin.collections.HashMap

const val KEY_SEPARATOR = "__separator__"

// using Map<String, ...> is much faster than Map<List<String>, ...>, benchmark for 100mil gets:
//   lists in 361ms
//   strings in 12ms

/**
 * A partial implementation of an I18N service.
 * @param T The type of elements that are generated by translation operations.
 * @param D The stateful data stored on this instance.
 */
abstract class AbstractI18N<T, D : AbstractI18N.TranslationData>(
    override val locale: Locale,
    private val translations: Map<String, Map<Locale, D>>
) : I18N<T> {
    /**
     * The data stored on a built [AbstractI18N] service.
     * @property template The template to generate with.
     */
    interface TranslationData {
        val template: Template

        /**
         * A simple implementation of a [TranslationData], only storing the template to generate with.
         */
        data class Simple(override val template: Template) : TranslationData
    }

    /**
     * Gets the translation data for a particular key and locale.
     */
    protected fun translation(key: String, locale: Locale) = translations[key]?.let { tl ->
        tl[locale] ?: tl[this.locale] ?: tl[Locale.ROOT]
    }

    /**
     * Throws a generic exception for failing to translate a key.
     */
    protected fun i18nException(key: String, locale: Locale, cause: Throwable? = null): Nothing {
        throw I18NException("Could not generate translation for $key / ${locale.toLanguageTag()}", cause)
    }

    /**
     * A partial implementation of an [AbstractI18N] builder.
     */
    abstract class Builder<T>(var locale: Locale) : I18N.Builder<T> {
        private val translations = ArrayList<Translation.Root>()

        override fun register(tl: Translation.Root) {
            translations.add(tl)
        }

        /**
         * Walks the translation tree and builds the [TranslationData] for each value node.
         * @param dataFactory The function to map a path and template to a [D].
         * @param D The [TranslationData] type.
         * @return The data, which can be passed to an [AbstractI18N].
         */
        protected fun <D : TranslationData> buildTranslationData(
            dataFactory: (List<String>, Template) -> D
        ): Map<String, Map<Locale, D>> {
            val res = HashMap<String, MutableMap<Locale, D>>()
            translations.forEach { tl ->
                tl.walk { path, child ->
                    if (child is Translation.Value) {
                        val template = try {
                            Template.parse(child.value)
                        } catch (ex: Template.ParsingException) {
                            throw I18NException("Failed to register '${path.joinToString(I18N_SEPARATOR)}'", ex)
                        }
                        res.computeIfAbsent(path.joinToString(I18N_SEPARATOR)) { HashMap() }[tl.locale] = dataFactory(path, template)
                    }
                }
            }
            return res
        }

        /**
         * A single part of a generated translation.
         */
        sealed interface Token<T> {
            /** The path to this token. */
            val path: List<String>

            /**
             * A token storing text.
             * @param value The text.
             */
            data class Text<T>(override val path: List<String>, val value: String) : Token<T>

            /**
             * A token storing an existing [T].
             * @param value The [T].
             */
            data class Raw<T>(override val path: List<String>, val value: T) : Token<T>
        }

        private fun makeTokens(
            locale: Locale,
            template: Template,
            args: Argument.ArgMap<T>.State,
            res: Lines<T>,
            path: List<String> = emptyList()
        ) {
            fun wrongType(factory: Argument.Factory<T>, template: Template, path: List<String>): Nothing =
                throw I18NException("$path: Argument/template type mismatch - argument '${factory.name}', template '${template.name}'")

            when (template) {
                is Template.Text -> {
                    res.addText(path, template.value)
                }
                is Template.Holder -> template.children.forEach {
                    makeTokens(locale, it, args, res)
                }
                is Template.Raw -> when (val factory = args.backing(template.key)) {
                    is Argument.Factory.Raw<T> -> {
                        val newPath = path + template.key

                        // build all ICU args
                        val icuArgs = HashMap<String, Any?>()
                        args.backing.args.forEach { (key, childFactory) ->
                            if (childFactory is Argument.Factory.Raw<T>) {
                                icuArgs[if (key == template.key) THIS_ARG else key] =
                                    args.compute<Argument.Raw<T>.State>(key, childFactory).backing.value
                            }
                        }

                        // make text
                        res.addText(newPath, MessageFormat("{${template.content}}", locale).build(icuArgs))
                    }
                    null -> {}
                    else -> wrongType(factory, template, path + template.key)
                }
                is Template.Substitution -> when (val factory = args.backing(template.key)) {
                    is Argument.Factory.Substitution<T> -> {
                        val newPath = path + template.key
                        val value = args.compute<Argument.Substitution<T>.State>(template.key, factory).backing.value

                        res.add(listOf(listOf(Token.Raw(newPath, value))))
                    }
                    null -> {}
                    else -> wrongType(factory, template, path + template.key)
                }
                is Template.Scope -> when (val factory = args.backing(template.key)) {
                    is Argument.Factory.Scoped<T> -> {
                        val newPath = path + template.key
                        when (val state = args.compute<Argument.State<T>>(template.key, factory)) {
                            is Argument.ArgMap<T>.State -> {
                                template.content.forEach { child ->
                                    makeTokens(locale, child, state, res, newPath)
                                }
                            }
                            is Argument.ArgList<T>.State -> {
                                val separator = Lines<T>()
                                val sepPath = newPath + KEY_SEPARATOR
                                template.separator.forEach {
                                    makeTokens(locale, it, args, separator, sepPath)
                                }

                                val children = state.compute()
                                children.forEachIndexed { idx, childState ->
                                    template.content.forEach { childTemplate ->
                                        makeTokens(locale, childTemplate, when (childState) {
                                            is Argument.ArgMap<T>.State -> childState
                                            else -> Argument.single(when (childState) {
                                                is Argument.Raw<T>.State -> Argument.Factory.Raw { Argument.Raw(childState.backing.value) }
                                                is Argument.Substitution<T>.State -> Argument.Factory.Substitution { Argument.Substitution(childState.backing.value) }
                                                is Argument.ArgList<T>.State -> Argument.Factory.Scoped { Argument.ArgList(childState.backing.args) }
                                                else -> throw IllegalStateException()
                                            }).createState()
                                        }, res, newPath)
                                    }

                                    if (idx < children.size - 1) {
                                        res.add(separator.lines)
                                    }
                                }
                            }
                        }
                    }
                    null -> {}
                    else -> wrongType(factory, template, path + template.key)
                }
            }
        }

        /**
         * Generates the lines of tokens of [T]s to be later converted to a list of [T]s.
         */
        protected fun makeTokens(
            i18n: I18N<T>,
            locale: Locale,
            template: Template,
            args: Argument.MapScope<T>.() -> Unit
        ): Lines<T> {
            val arg = try {
                Argument.buildMap(i18n.withLocale(locale), args)
            } catch (ex: KeyValidationException) {
                throw I18NException("Invalid key", ex)
            }
            val res = Lines<T>()
            makeTokens(locale, template, arg.State(), res)
            return res
        }

        /**
         * A wrapper class around a List<List<T>>, allowing an easy way to combine lines of tokens.
         */
        @JvmInline
        protected value class Lines<T>(val lines: MutableList<MutableList<Token<T>>> = ArrayList()) {
            fun add(other: Iterable<Iterable<Token<T>>>) {
                if (!other.any())
                    return
                if (lines.isEmpty()) {
                    lines.addAll(other.map { it.toMutableList() })
                } else {
                    lines.last().addAll(other.first())
                    lines.addAll(other.drop(1).map { it.toMutableList() })
                }
            }

            fun addText(path: List<String>, value: String) {
                add(value.split('\n').map { listOf(Token.Text(path, it)) })
            }
        }
    }
}

/**
 * Builds a message format to a string with the passed arguments.
 */
fun MessageFormat.build(args: Map<String, Any?>): String {
    val res = StringBuffer()
    format(args, res, FieldPosition(0))
    return res.toString()
}
