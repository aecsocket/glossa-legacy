package com.github.aecsocket.glossa.core

import com.github.aecsocket.glossa.core.extension.build
import com.ibm.icu.text.MessageFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

open class I18NException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

const val THIS_ARG = "_"

interface I18NContext<T> {
    fun make(key: Iterable<String>, args: Argument.MapScope<T>.() -> Unit = {}): List<T>?

    fun make(vararg key: String, args: Argument.MapScope<T>.() -> Unit = {}) = make(key.asIterable(), args)

    fun safe(key: Iterable<String>, args: Argument.MapScope<T>.() -> Unit = {}): List<T>

    fun safe(vararg key: String, args: Argument.MapScope<T>.() -> Unit = {}) = safe(key.asIterable(), args)
}

abstract class I18N<T>(
    var locale: Locale
) : I18NContext<T> {
    private val translations = HashMap<Locale, Translation.Root>()
    private val cache = HashMap<Locale, MutableMap<List<String>, Template?>>()

    abstract fun make(locale: Locale = this.locale, key: Iterable<String>, args: Argument.MapScope<T>.() -> Unit = {}): List<T>?

    fun make(locale: Locale = this.locale, vararg key: String, args: Argument.MapScope<T>.() -> Unit = {}) =
        make(locale, key.asIterable(), args)

    override fun make(key: Iterable<String>, args: Argument.MapScope<T>.() -> Unit) = make(locale, key, args)

    override fun make(vararg key: String, args: Argument.MapScope<T>.() -> Unit): List<T>? = make(locale, *key, args = args)


    abstract fun safe(locale: Locale = this.locale, key: Iterable<String>, args: Argument.MapScope<T>.() -> Unit = {}): List<T>

    fun safe(locale: Locale = this.locale, vararg key: String, args: Argument.MapScope<T>.() -> Unit = {}) =
        safe(locale, key.asIterable(), args)

    override fun safe(key: Iterable<String>, args: Argument.MapScope<T>.() -> Unit) = safe(locale, key, args)

    override fun safe(vararg key: String, args: Argument.MapScope<T>.() -> Unit) = safe(locale, *key, args = args)

    fun contextOf(locale: Locale) = object : I18NContext<T> {
        override fun make(key: Iterable<String>, args: Argument.MapScope<T>.() -> Unit) = make(locale, key, args)
        override fun safe(key: Iterable<String>, args: Argument.MapScope<T>.() -> Unit) = safe(locale, key, args)
    }

    fun register(tl: Translation.Root) {
        translations[tl.locale] = translations[tl.locale]?.let {
            it + tl
        } ?: tl
    }

    fun register(locale: Locale, content: Translation.Scope.() -> Unit) = register(Translation.buildRoot(locale, content))

    open fun clear() {
        translations.clear()
        cache.clear()
    }

    protected fun translation(locale: Locale, key: Iterable<String>) = translations[locale]?.get(key)
        ?: if (locale != this.locale) translations[this.locale]?.get(key)
        else if (locale != Locale.ROOT) translations[Locale.ROOT]?.get(key)
        else null

    protected fun template(locale: Locale, key: Iterable<String>) = cache
        .computeIfAbsent(locale) { HashMap() }
        .computeIfAbsent(key.toList()) { translation(locale, it)?.let { tl ->
            if (tl is Translation.Value) Template.parse(tl.value) else null
        } }

    sealed interface Token<T> {
        val path: List<String>

        data class Text<T>(override val path: List<String>, val value: String) : Token<T>

        data class Raw<T>(override val path: List<String>, val value: T) : Token<T>
    }

    private fun makeTokens(
        locale: Locale,
        template: Template,
        args: Argument.ArgMap<T>.State,
        res: Lines<Token<T>>,
        path: List<String> = emptyList()
    ) {
        fun wrongType(factory: Argument.Factory<T>, template: Template): Nothing =
            throw I18NException("Invalid argument factory type ${factory.name} for template type '${template.name}'")

        fun Lines<Token<T>>.addText(value: String) {
            add(value.split('\n').map { listOf(Token.Text(path, it)) })
        }

        when (template) {
            is Template.Text -> {
                res.addText(template.value)
            }
            is Template.Holder -> template.children.forEach {
                makeTokens(locale, it, args, res)
            }
            is Template.Raw -> when (val factory = args.backing(template.key)) {
                is Argument.Factory.Raw<T> -> {
                    // build all ICU args
                    val icuArgs = HashMap<String, Any?>()
                    args.backing.args.forEach { (key, childFactory) ->
                        if (childFactory is Argument.Factory.Raw<T>) {
                            icuArgs[if (key == template.key) THIS_ARG else key] =
                                args.compute<Argument.Raw<T>.State>(key, childFactory).backing.value
                        }
                    }

                    // make text
                    res.addText(MessageFormat("{${template.content}}", locale).build(icuArgs))
                }
                null -> {}
                else -> wrongType(factory, template)
            }
            is Template.Substitution -> when (val factory = args.backing(template.key)) {
                is Argument.Factory.Substitution<T> -> {
                    val lines = args.compute<Argument.Substitution<T>.State>(template.key, factory).backing.value

                    val separator = Lines<Token<T>>()
                    template.separator.forEach {
                        makeTokens(locale, it, args, separator)
                    }

                    lines.forEachIndexed { idx, line ->
                        res.add(listOf(listOf(Token.Raw(path, line))))
                        if (idx < lines.size - 1)
                            res.add(separator.lines)
                    }
                }
                null -> {}
                else -> wrongType(factory, template)
            }
            is Template.Scope -> when (val factory = args.backing(template.key)) {
                is Argument.Factory.Scoped<T> -> {
                    when (val state = args.compute<Argument.State<T>>(template.key, factory)) {
                        is Argument.ArgMap<T>.State -> {
                            template.content.forEach { child ->
                                makeTokens(locale, child, state, res)
                            }
                        }
                        is Argument.ArgList<T>.State -> {
                            state.compute().forEach { childState -> template.content.forEach { childTemplate ->
                                makeTokens(locale, childTemplate, when (childState) {
                                    is Argument.ArgMap<T>.State -> childState
                                    else -> Argument.single(when (childState) {
                                        is Argument.Raw<T>.State -> Argument.Factory.Raw { Argument.Raw(childState.backing.value) }
                                        is Argument.Substitution<T>.State -> Argument.Factory.Substitution { Argument.Substitution(childState.backing.value) }
                                        is Argument.ArgList<T>.State -> Argument.Factory.Scoped { Argument.ArgList(childState.backing.args) }
                                        else -> throw IllegalStateException()
                                    }).createState()
                                }, res)
                            } }
                        }
                    }
                }
                null -> {}
                else -> wrongType(factory, template)
            }
        }
    }

    protected fun makeTokens(
        locale: Locale,
        template: Template,
        args: Argument.MapScope<T>.() -> Unit
    ): Lines<Token<T>> {
        val arg = try {
            Argument.buildMap(this, locale, args)
        } catch (ex: KeyValidationException) {
            throw I18NException("Invalid key", ex)
        }
        val res = Lines<Token<T>>()
        makeTokens(locale, template, arg.State(), res)
        return res
    }
}

interface Localizable<T> {
    fun localize(ctx: I18NContext<T>): List<T>
}

const val KEY_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_"

class KeyValidationException(string: String, val index: Int)
    : RuntimeException("Invalid character '${string[index]}' in '$string' at position ${index+1}, valid: [$KEY_CHARS]")

fun validateKey(key: String): String {
    val idx = key.indexOfFirst { !KEY_CHARS.contains(it) }
    if (idx != -1)
        throw KeyValidationException(key, idx)
    return key
}

@JvmInline
value class Lines<T>(val lines: MutableList<MutableList<T>> = ArrayList()) {
    fun add(other: Iterable<Iterable<T>>) {
        if (!other.any())
            return
        if (lines.isEmpty()) {
            lines.addAll(other.map { it.toMutableList() })
        } else {
            lines.last().addAll(other.first())
            lines.addAll(other.drop(1).map { it.toMutableList() })
        }
    }
}
