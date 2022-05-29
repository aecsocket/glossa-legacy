package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.text.FieldPosition
import java.util.*
import kotlin.collections.HashMap

const val KEY_SEPARATOR = "__separator__"

// using Map<String, ...> is much faster than Map<List<String>, ...>, benchmark for 100mil gets:
//   lists in 361ms
//   strings in 12ms
abstract class AbstractI18N<T, D : AbstractI18N.TranslationData>(
    override val locale: Locale,
    private val translations: Map<String, Map<Locale, D>>
) : I18N<T> {
    interface TranslationData {
        val template: Template

        data class Simple(override val template: Template) : TranslationData
    }

    protected fun translation(key: String, locale: Locale) = translations[key]?.let { tl ->
        tl[locale] ?: tl[this.locale] ?: tl[Locale.ROOT]
    }

    abstract class Builder<T>(var locale: Locale) : I18N.Builder<T> {
        private val translations = ArrayList<Translation.Root>()

        override fun register(tl: Translation.Root) {
            translations.add(tl)
        }

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
                            throw I18NException("Failed to register '${path.i18nPath()}'", ex)
                        }
                        res.computeIfAbsent(path.i18nPath()) { HashMap() }[tl.locale] = dataFactory(path, template)
                    }
                }
            }
            return res
        }

        sealed interface Token<T> {
            val path: String

            data class Text<T>(override val path: String, val value: String) : Token<T>

            data class Raw<T>(override val path: String, val value: T) : Token<T>
        }

        private fun makeTokens(
            locale: Locale,
            template: Template,
            args: Argument.ArgMap<T>.State,
            res: Lines<T>,
            path: String = ""
        ) {
            fun wrongType(factory: Argument.Factory<T>, template: Template): Nothing =
                throw I18NException("Invalid argument factory type ${factory.name} for template type '${template.name}'")

            fun addPath(next: String, base: String = path) = if (next == THIS_ARG) base else
                (if (base.isEmpty()) "" else "$base.") + next

            when (template) {
                is Template.Text -> {
                    res.addText(path, template.value)
                }
                is Template.Holder -> template.children.forEach {
                    makeTokens(locale, it, args, res)
                }
                is Template.Raw -> when (val factory = args.backing(template.key)) {
                    is Argument.Factory.Raw<T> -> {
                        val newPath = addPath(template.key)

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
                    else -> wrongType(factory, template)
                }
                is Template.Substitution -> when (val factory = args.backing(template.key)) {
                    is Argument.Factory.Substitution<T> -> {
                        val newPath = addPath(template.key)
                        val lines = args.compute<Argument.Substitution<T>.State>(template.key, factory).backing.value

                        val separator = Lines<T>()
                        val sepPath = addPath(KEY_SEPARATOR, newPath)
                        template.separator.forEach {
                            makeTokens(locale, it, args, separator, sepPath)
                        }

                        lines.forEachIndexed { idx, line ->
                            res.add(listOf(listOf(Token.Raw(newPath, line))))
                            if (idx < lines.size - 1) {
                                res.add(separator.lines)
                            }
                        }
                    }
                    null -> {}
                    else -> wrongType(factory, template)
                }
                is Template.Scope -> when (val factory = args.backing(template.key)) {
                    is Argument.Factory.Scoped<T> -> {
                        val newPath = addPath(template.key)
                        when (val state = args.compute<Argument.State<T>>(template.key, factory)) {
                            is Argument.ArgMap<T>.State -> {
                                template.content.forEach { child ->
                                    makeTokens(locale, child, state, res, newPath)
                                }
                            }
                            is Argument.ArgList<T>.State -> {
                                val separator = Lines<T>()
                                val sepPath = addPath(KEY_SEPARATOR, newPath)
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
                    else -> wrongType(factory, template)
                }
            }
        }

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

            fun addText(path: String, value: String) {
                add(value.split('\n').map { listOf(Token.Text(path, it)) })
            }
        }
    }
}

fun MessageFormat.build(args: Map<String, Any?>): String {
    val res = StringBuffer()
    format(args, res, FieldPosition(0))
    return res.toString()
}
