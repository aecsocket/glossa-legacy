package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/** Token for the current scope element in a format. */
const val THIS = "_"
/** Separator for path entries of [FormatToken]s. */
const val FORMAT_TOKEN_SEPARATOR = "."

sealed interface FormatToken<E> {
    val path: List<String>

    fun path() = path.joinToString(FORMAT_TOKEN_SEPARATOR)
}

data class StringToken<E>(
    val value: String,
    override val path: List<String>
) : FormatToken<E>

data class RawToken<E>(
    val value: E,
    override val path: List<String>
) : FormatToken<E>

data class ArgumentInstance<E>(
    val handle: TemplatingI18N.ArgumentsMap<E>,
    val built: MutableMap<String, TemplatingI18N.Argument<E>?> = HashMap()
) {
    operator fun get(key: String) = built.computeIfAbsent(key) { handle.args[it]?.create() }
}

/**
 * Partial implementation of an I18N service which uses the [Templating] methods
 * to perform formatting.
 *
 * Handles caching template nodes and provides utilities to format using [Templating].
 */
abstract class TemplatingI18N<E>(
    locale: Locale = Locale.ROOT
) : AbstractI18N<List<E>, TemplatingI18N.ArgumentsMap<E>>(locale) {
    private val cache = HashMap<Locale, MutableMap<String, TemplateNode?>>()

    /**
     * Helper method to generate format tokens for a get operation.
     * @param locale the locale.
     * @param key the localization key.
     * @param args the args.
     * @return the lines of format tokens.
     */
    protected fun format(locale: Locale, key: String, args: ArgumentsMap<E>): List<List<FormatToken<E>>>? {
        return cache.computeIfAbsent(locale) { HashMap() }.computeIfAbsent(key) {
            translation(locale, key)?.let { Templating.parse(it) }
        }?.let { node ->
            format(locale, node, ArgumentInstance(args)).lines
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
     * Formats a node generated from [parseChildren] into lines of tokens.
     *
     * **Node**
     *
     *     ├─ <Actions on {date, date, long}: >
     *     └─ @action []
     *         ├─ <>
     *         │  <  {purchases, plural, one {# purchase:} other {# purchases:}} >
     *         └─ @entry []
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
    protected fun format(
        locale: Locale,
        node: TemplateNode,
        args: ArgumentInstance<E>,
        path: List<String> = emptyList()
    ): Lines<FormatToken<E>> {
        fun join(tokens: List<Lines<FormatToken<E>>>, separator: String): Lines<FormatToken<E>> {
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
                args.handle.args.forEach { (key, value) ->
                    // for every pre-computed arg here (ones that make a RawArgument, basically)
                    // we compute them here, so we can use them in the ICU MessageFormat

                    // we *could* only compute the templates that appear in the msg
                    //     node.value.contains("{$key")
                    // but that's hacky. maybe try this later?
                    if (value is ComputedArgumentFactory<E>) {
                        built[key] = when (val arg = args[key]) {
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
                fun handle(arg: Argument<E>): Lines<FormatToken<E>> = when (arg) {
                    // this is handled when it's a TextNode
                    // but on its own, don't do anything
                    is RawArgument<E> -> Lines()
                    is ArgumentsMap<E> -> {
                        val lines = Lines<FormatToken<E>>()
                        val newPath = path + node.key
                        node.children.forEach { child ->
                            lines += format(locale, child, ArgumentInstance(arg), newPath)
                        }
                        lines
                    }
                    is ArgumentsList<E> -> join(arg.args.map { handle(it) }, node.separator)
                    else -> Lines() // strict opportunity
                }

                args[node.key]?.let { handle(it) } ?: Lines() // strict opportunity
            }
            is SubstitutionNode -> {
                fun subst(value: List<E>) =
                    join(value.map { Lines(mutableListOf(mutableListOf(RawToken(it, path)))) }, node.separator)

                args[node.key]?.let { arg ->
                    when (arg) {
                        is SubstitutionArgument<E> -> subst(arg.value)
                        is LocalizedArgument<E> -> subst(arg.value.localize(this, locale))
                        else -> Lines() // strict opportunity
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

        /*node.children.forEach {
            lines += format(locale, it, args, path)
        }*/


        /*is TextNode -> {
            val builtArgs = HashMap<String, Any?>()
            args.forEach { key, value ->
                /*
                Other method: invoke the arg only if it appears as an ICU template
                But this is relatively hacky. Instead, we'll just invoke all args in the current scope.
                if (node.value.contains("{$key")) { // ICU template start
                    builtArgs[key] = value.invoke()
                }

                 */
                builtArgs[key] = value.invoke()
            }
            Lines.fromText(MessageFormat(node.value, locale).asString(builtArgs)) { Templating.FormatToken(path, it) }
        }
        is ScopeNode -> {
            fun linesOfArgs(args: Args): Lines<Templating.FormatToken> {
                var lines = Lines<Templating.FormatToken>()
                node.children.forEach { child ->
                    lines += format(locale, child, args, path + node.label)
                }
                return lines
            }

            args[node.label]?.let { valueFactory ->
                fun handle(value: Any): Lines<Templating.FormatToken> = when (value) {
                    is MultiArgs -> {
                        var totalLines = Lines<Templating.FormatToken>()
                        val childLines = value.args.map { handle(it()) }
                        val separator = Lines.fromText(node.separator) {
                            Templating.FormatToken(
                                path + node.label + SEPARATOR,
                                it
                            )
                        }
                        childLines.forEachIndexed { idx, lines ->
                            totalLines += lines
                            if (idx < childLines.size - 1) {
                                totalLines += separator
                            }
                        }
                        totalLines
                    }
                    is Args -> linesOfArgs(value)
                    else -> linesOfArgs(Args(mapOf(THIS to { value })))
                }

                handle(valueFactory())
            } ?: Lines()
        }
        else -> {
            var lines = Lines<Templating.FormatToken>()
            node.children.forEach {
                lines += format(locale, it, args, path)
            }
            lines
        }*/

    sealed interface Argument<E> {
        companion object {
            @JvmStatic fun <E> empty() = args<E>()
        }
    }

    sealed interface ArgumentFactory<E> {
        fun create(): Argument<E>
    }

    fun interface ComputedArgumentFactory<E> : ArgumentFactory<E> {
        override fun create(): RawArgument<E>
    }

    fun interface LazyArgumentFactory<E> : ArgumentFactory<E>


    data class RawArgument<E>(val value: Any) : Argument<E>

    data class SubstitutionArgument<E>(val value: List<E>) : Argument<E>

    data class LocalizedArgument<E>(val value: Localizable<E>) : Argument<E>

    data class ArgumentsMap<E>(val args: Map<String, ArgumentFactory<E>>) : Argument<E>

    data class ArgumentsList<E>(val args: List<Argument<E>>) : Argument<E>

    interface Localizable<E> {
        fun localize(i18n: TemplatingI18N<E>, locale: Locale): List<E>
    }

    companion object {
        @JvmStatic
        fun <E> arg(value: Any) = RawArgument<E>(value)

        @JvmStatic
        fun <E> argSub(value: List<E>) = SubstitutionArgument(value)

        @JvmStatic
        fun <E> argTl(value: Localizable<E>) = LocalizedArgument(value)

        @JvmStatic
        fun <E> args(value: Map<String, ArgumentFactory<E>>) = ArgumentsMap(value)

        @JvmStatic
        fun <E> args(vararg args: Pair<String, ArgumentFactory<E>>) = args(args.associate { it })

        @JvmStatic
        fun <E> argList(value: List<Argument<E>>) = ArgumentsList(value)

        @JvmStatic
        fun <E> argList(vararg args: Argument<E>) = argList(args.toList())

        infix fun <E> String.arg(value: Any) =
            Pair(this, ComputedArgumentFactory { TemplatingI18N.arg<E>(value) })

        infix fun <E> String.argSub(value: () -> List<E>) =
            Pair(this, LazyArgumentFactory { argSub(value()) })

        infix fun <E> String.argTl(value: () -> Localizable<E>) =
            Pair(this, LazyArgumentFactory { argTl(value()) })

        infix fun <E> String.args(value: () -> Map<String, ArgumentFactory<E>>) =
            Pair(this, LazyArgumentFactory { args(value()) })

        infix fun <E> String.argList(value: () -> List<Argument<E>>) =
            Pair(this, LazyArgumentFactory { argList(value()) })

        fun test() {
            data class SubItem(
                val id: String,
                val count: Int
            ) : Localizable<String> {
                override fun localize(i18n: TemplatingI18N<String>, locale: Locale) =
                    i18n.safe(locale, "item.$id")
            }

            /*
            Actions: @<purchase>[
              {total plural, one {# purchase} other {# purchases}}: @<entry>[
                - "@$<item>[]" x{amount, number}]]
             */

            val locale = Locale.US
            val i18n = StringI18N(locale)
            args<String>(
                "details" args {mapOf(
                    "amount" arg 12_345.6
                )},
                "sub" argSub {listOf("hello")},
                "localized" argTl {SubItem("my_item", 5)},
                "localized_sub" argSub {SubItem("my_item", 5).localize(i18n, locale)},
                "purchase" argList {listOf(args(
                    "total" arg 5,
                    "entry" argList {listOf(args(
                        "name" arg "Item one",
                        "amount" arg 3
                    ), args(
                        "name" arg "Item two",
                        "amount" arg 2
                    ))}
                ), args(
                    "total" arg 1,
                    "entry" argList {listOf(args(
                        "name" arg "Item",
                        "amount" arg 1
                    ))}
                ))}
            )

            args<String>(
                "details" to LazyArgumentFactory { args(
                    "amount" to ComputedArgumentFactory { arg(12_345.6) }
                ) }
            )
        }
    }
}

@JvmInline
value class Lines<T : Any>(val lines: MutableList<MutableList<T>> = ArrayList()) {
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
            todo convert to javadoc
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
