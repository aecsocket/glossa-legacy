package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/** Token for the current scope element in a format. */
const val THIS = "_"
/** Separator for path entries of [FormatToken]s. */
const val FORMAT_TOKEN_SEPARATOR = "."

/**
 * A token generated from parsing a node.
 * @param E the element type of the translation target.
 * @property path the path from the root to this node.
 */
sealed interface FormatToken<E> {
    val path: List<String>

    /**
     * Joins the path using [FORMAT_TOKEN_SEPARATOR].
     * @return the joined path.
     */
    fun path() = path.joinToString(FORMAT_TOKEN_SEPARATOR)
}

/**
 * A token storing a string value.
 * @param value the string value.
 */
data class StringToken<E>(
    val value: String,
    override val path: List<String>
) : FormatToken<E>

/**
 * A token storing a value of the translation target type.
 * @param value the translation target value.
 */
data class RawToken<E>(
    val value: E,
    override val path: List<String>
) : FormatToken<E>

/**
 * Partial implementation of an I18N service which uses the [Templating] methods
 * to perform formatting.
 *
 * Handles caching template nodes and provides utilities to format using [Templating].
 *
 * The steps taken for localization are:
 * 1. Look up the format string for the given locale and key, using fallback locale if needed
 * 2. (Cached) Convert the format string to a [TemplateNode] representation
 * 3. Using the [ArgumentMap] and [TemplateNode], generate the lines of [FormatToken]s.
 * 4. These are converted to [E] lists based on implementation.
 */
abstract class TemplatingI18N<E>(
    locale: Locale = Locale.ROOT
) : AbstractI18N<List<E>, TemplatingI18N.ArgumentMap<E>>(locale) {
    private val cache = HashMap<Locale, MutableMap<String, TemplateNode?>>()

    /**
     * Helper method to generate format tokens for a get operation.
     * @param locale the locale.
     * @param key the localization key.
     * @param args the args.
     * @return the lines of format tokens.
     */
    protected fun format(locale: Locale, key: String, args: ArgumentMap<E>): List<List<FormatToken<E>>>? {
        return cache.computeIfAbsent(locale) { HashMap() }.computeIfAbsent(key) {
            translation(locale, key)?.let { Templating.parse(it) }
        }?.let { node ->
            format(locale, node, args.createState()).lines
        }
    }

    /**
     * Generates a list of [E] localization based on the locale and key passed.
     * No args are provided.
     *
     * Returns `null` if the key was not found in the translation or fallback
     * locale's translation.
     *
     * @param locale the locale to generate with.
     * @param key the localization key.
     * @return the translation.
     */
    operator fun get(locale: Locale, key: String) = get(locale, key, Argument.empty())

    /**
     * Generates a list of [E] localization based on the key passed.
     * Uses the fallback locale [locale].
     * No args are provided.
     *
     * Returns `null` if the key was not found in the translation or fallback
     * locale's translation.
     *
     * @param key the localization key.
     * @return the translation.
     */
    operator fun get(key: String) = get(key, Argument.empty())

    /**
     * Generates a list of [E] localization based on the locale and key passed.
     * No args are provided.
     *
     * If the key was not found, generates a default list of [E] based on the implementation.
     *
     * @param locale the locale to generate with.
     * @param key the localization key.
     * @return the translation.
     */
    fun safe(locale: Locale, key: String) = safe(locale, key, Argument.empty())

    /**
     * Generates a list of [E] localization based on the key passed.
     * Uses the fallback locale [locale].
     * No args are provided.
     *
     * If the key was not found, generates a default list of [E] based on the implementation.
     *
     * @param key the localization key.
     * @return the translation.
     */
    fun safe(key: String) = safe(key, Argument.empty())

    override fun clear() {
        super.clear()
        cache.clear()
    }

    /**
     * Formats a node generated from [Templating.parseChildren] into lines of tokens.
     *
     * **Node**
     *
     *     ├─ <Actions on {date, date, long}: >
     *     └─ @<action> []
     *         ├─ <>
     *         │  <  {purchases, plural, one {# purchase:} other {# purchases:}} >
     *         └─ @<entry> []
     *             └─ <>
     *                <    - {item} x{amount}>
     *
     * **Arguments**
     * ```json
     * {
     *   "date": "01/01/1970",
     *   "action": [
     *     {
     *       "purchases": 5,
     *       "entry": [
     *         { "item": "Item One", "amount": 3 },
     *         { "item": "Item Two", "amount": 2 }
     *       ]
     *     },
     *     {
     *       "purchases": 1,
     *       "entry": [
     *         { "item": "Some Item", "amount": 1 }
     *       ]
     *     }
     *   ]
     * }
     * ```
     * **Result**
     *
     *     Actions on January 1, 1970:
     *       5 purchases:
     *         - Item One x3
     *         - Item Two x2
     *       1 purchase:
     *         - Some Item x1
     *
     * @param locale the locale to generate for, used in [MessageFormat.format].
     * @param node the node to generate with.
     * @param args the arguments.
     * @param path the current depth of the node.
     * @return the lines of tokens.
     */
    private fun format(
        locale: Locale,
        node: TemplateNode,
        args: ArgumentMap<E>.State,
        path: List<String> = emptyList()
    ): Lines<FormatToken<E>> {
        fun join(path: List<String>, tokens: List<Lines<FormatToken<E>>>, separator: String): Lines<FormatToken<E>> {
            val sepPath = path + SEPARATOR
            val separatorLines = separator.split('\n').map { listOf(StringToken<E>(it, sepPath)) }

            val lines = Lines<FormatToken<E>>()
            tokens.forEachIndexed { idx, line ->
                lines += line
                if (idx < tokens.size - 1) {
                    lines += separatorLines
                }
            }
            return lines
        }

        return when (node) {
            is TextNode -> {
                val built = HashMap<String, Any?>()
                args.forEach { key, value ->
                    // compute args only if it's seen as an ICU message format
                    if (node.value.contains("{$key")) {
                        built[key] = when (val arg = args[key]?.arg) {
                            is RawArgument<E> -> arg.value
                            else -> null
                        }
                    }
                }
                val formatted = MessageFormat(node.value, locale).asString(built)
                // TODO I don't like this `as FormatToken<E>`
                Lines(formatted.split('\n').map { mutableListOf(StringToken<E>(it, path) as FormatToken<E>) }
                    .toMutableList())
            }
            // TODO strict opportunities
            is ScopeNode -> {
                args[node.key]?.let {
                    val newPath = path + node.key
                    fun handle(state: Argument.State<E>): Lines<FormatToken<E>> = when (state) {
                        is ArgumentMap<E>.State -> {
                            val lines = Lines<FormatToken<E>>()
                            node.children.forEach { child ->
                                lines += format(locale, child, state, newPath)
                            }
                            lines
                        }
                        is ArgumentList<E>.State -> join(newPath, state.cache.map { handle(it) }, node.separator)
                        else -> when (val arg = state.arg) {
                            is RawArgument<E> -> {
                                val lines = Lines<FormatToken<E>>()
                                val valueState = argMap(THIS to {arg}).createState()
                                node.children.forEach { child ->
                                    lines += format(locale, child, valueState, newPath)
                                }
                                lines
                            }
                            else -> Lines()
                        } // strict opportunity
                    }

                    handle(it)
                } ?: Lines() // strict opportunity
            }
            is SubstitutionNode -> {
                args[node.key]?.let { state ->
                    val newPath = path + node.key
                    fun subst(value: List<E>) =
                        join(newPath, value.map { Lines(mutableListOf(mutableListOf(RawToken(it, newPath)))) }, node.separator)

                    when (state) {
                        is LocalizedArgument<E>.State -> subst(state.value(this, locale))
                        else -> when (val arg = state.arg) {
                            is SubstitutionArgument<E> -> subst(arg.value)
                            else -> Lines() // strict opportunity
                        }
                    }
                } ?: Lines() // strict opportunity
            }
            is RootNode -> {
                val lines = Lines<FormatToken<E>>()
                node.children.forEach { lines += format(locale, it, args, path) }
                lines
            }
        }
    }

    /**
     * A value which can be replaced in a format string.
     * @param E the element type of the translation target.
     */
    sealed interface Argument<E> {
        /**
         * The mutable state of an argument, currently being used for localization.
         * @param E the element type of the translation target.
         * @property arg the backing argument.
         */
        interface State<E> {
            val arg: Argument<E>
        }

        /**
         * A state which stores no state other than the backing argument.
         */
        data class NoState<E>(override val arg: Argument<E>) : State<E>

        /**
         * Creates a blank state object from this argument.
         * @return the state.
         */
        fun createState(): State<E>

        companion object {
            /**
             * Creates an argument with no function.
             * @return the argument.
             */
            @JvmStatic fun <E> empty() = argMap<E>()
        }
    }

    /**
     * An argument representing a raw value passed to ICU [MessageFormat].
     *
     * Creates [Argument.NoState] state.
     * @property value the raw value.
     */
    data class RawArgument<E>(val value: Any) : Argument<E> {
        override fun createState() = Argument.NoState(this)
    }

    /**
     * An argument representing a value of type List<[E]>.
     *
     * Creates [Argument.NoState] state.
     * @property value the list of [E]s.
     */
    data class SubstitutionArgument<E>(val value: List<E>) : Argument<E> {
        override fun createState() = Argument.NoState(this)
    }

    /**
     * An argument representing a [Localizable] object which can be converted to List<[E]>.
     *
     * Creates [LocalizedArgument.State] state.
     * @property value the localizable.
     */
    data class LocalizedArgument<E>(val value: Localizable<E>) : Argument<E> {
        inner class State : Argument.State<E> {
            override val arg: Argument<E>
                get() = this@LocalizedArgument

            private var cache: List<E>? = null

            fun value(i18n: TemplatingI18N<E>, locale: Locale) = cache
                ?: value.localize(i18n, locale).also { cache = it }
        }

        override fun createState() = State()
    }

    /**
     * An argument representing a key-value map of strings to [Argument] creators.
     *
     * Creates [ArgumentMap.State] state.
     * @property args the arguments.
     */
    data class ArgumentMap<E>(val args: Map<String, () -> Argument<E>>) : Argument<E> {
        inner class State(private val cache: MutableMap<String, Argument.State<E>?> = HashMap()) : Argument.State<E> {
            override val arg: Argument<E>
                get() = this@ArgumentMap

            operator fun get(key: String) = cache.computeIfAbsent(key) { args[it]?.invoke()?.createState() }

            fun forEach(action: (String, Argument.State<E>) -> Unit) {
                args.forEach { (key, _) ->
                    get(key)?.let { action(key, it) }
                }
            }
        }

        override fun createState() = State()

        operator fun plus(o: Map<String, () -> Argument<E>>) = ArgumentMap(args + o)

        operator fun plus(o: Pair<String, () -> Argument<E>>) = ArgumentMap(args + o)

        operator fun plus(o: ArgumentMap<E>) = ArgumentMap(args + o.args)
    }

    /**
     * An argument representing a list of [Argument]s.
     *
     * Creates [ArgumentList.State] state.
     * @property args the arguments.
     */
    data class ArgumentList<E>(val args: List<Argument<E>>) : Argument<E> {
        inner class State : Argument.State<E> {
            override val arg: Argument<E>
                get() = this@ArgumentList

            val cache: Array<Argument.State<E>> by lazy {
                Array(args.size) { args[it].createState() }
            }
        }

        override fun createState() = State()

        operator fun plus(o: List<Argument<E>>) = ArgumentList(args + o)

        operator fun plus(o: Argument<E>) = ArgumentList(args + o)

        operator fun plus(o: ArgumentList<E>) = ArgumentList(args + o.args)
    }
}

/**
 * An object which can be interpreted as a list of [E]s, as a result
 * of localizing this object.
 */
interface Localizable<E> {
    /**
     * Generates the translated version of this object.
     * @param i18n the I18N service used for translation.
     * @param locale the locale used.
     * @return the list of [E] translation targets.
     */
    fun localize(i18n: TemplatingI18N<E>, locale: Locale): List<E>
}

/** @see TemplatingI18N.RawArgument */
fun <E> arg(value: Any) = TemplatingI18N.RawArgument<E>(value)

/** @see TemplatingI18N.SubstitutionArgument */
fun <E> argSub(value: List<E>) = TemplatingI18N.SubstitutionArgument(value)

/** @see TemplatingI18N.LocalizedArgument */
fun <E> argTl(value: Localizable<E>) = TemplatingI18N.LocalizedArgument(value)

/** @see TemplatingI18N.ArgumentMap */
fun <E> argMap(value: Map<String, () -> TemplatingI18N.Argument<E>>) = TemplatingI18N.ArgumentMap(value)

/** @see TemplatingI18N.ArgumentMap */
fun <E> argMap(vararg args: Pair<String, () -> TemplatingI18N.Argument<E>>) = argMap(args.associate { it })

/** @see TemplatingI18N.ArgumentList */
fun <E> argList(value: List<TemplatingI18N.Argument<E>>) = TemplatingI18N.ArgumentList(value)

/** @see TemplatingI18N.ArgumentList */
fun <E> argList(vararg args: TemplatingI18N.Argument<E>) = argList(args.toList())

/** @see arg */
infix fun <E> String.arg(value: () -> Any) =
    Pair(this) { arg<E>(value()) }

/** @see argSub */
infix fun <E> String.argSub(value: () -> List<E>) =
    Pair(this) { argSub(value()) }

/** @see argTl */
infix fun <E> String.argTl(value: () -> Localizable<E>) =
    Pair(this) { argTl(value()) }

/** @see argMap */
infix fun <E> String.argMap(value: () -> Map<String, () -> TemplatingI18N.Argument<E>>) =
    Pair(this) { argMap(value()) }

/** @see argList */
infix fun <E> String.argList(value: () -> List<TemplatingI18N.Argument<E>>) =
    Pair(this) { argList(value()) }

@JvmInline
private value class Lines<T : Any>(val lines: MutableList<MutableList<T>> = ArrayList()) {
    fun add(other: List<List<T>>) {
        if (other.isEmpty())
            return
        if (lines.isEmpty()) {
            lines.addAll(other.map { it.toMutableList() })
        } else {
            /*
            [
              [ "hello", "world" ]
              [ "one" ]
            ]
              +
            [
              [ "two" ]
              [ "newline" ]
            ]
              =
            [
              [ "hello", "world" ]
              [ "one", "two" ]
              [ "newline" ]
            ]
             */
            lines.last().addAll(other.first())
            lines.addAll(other.subList(1, other.size).map { it.toMutableList() })
        }
    }

    operator fun plusAssign(other: List<List<T>>) =
        add(other)

    operator fun plusAssign(other: Lines<T>) =
        add(other.lines)
}
