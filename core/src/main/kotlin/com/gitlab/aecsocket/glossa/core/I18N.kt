package com.gitlab.aecsocket.glossa.core

import java.util.Locale

private const val LIST_SEPARATOR = "list_separator"

/**
 * Arguments for an I18N translation operation.
 * @param T The translation operation resulting type.
 * @param subst The substitution arguments.
 * @param icu The ICU arguments.
 */
data class I18NArgs<T>(
    val subst: Map<String, T>,
    val icu: Map<String, Any>,
) {
    /**
     * Merges this set of arguments with another.
     */
    operator fun plus(other: I18NArgs<T>) = I18NArgs(
        subst + other.subst,
        icu + other.icu
    )

    /**
     * The DSL scope for building an [I18NArgs] instance.
     * @param T The translation operation resulting type.
     */
    class Scope<T>(override val i18n: I18N<T>) : ForwardingI18N<T> {
        private val subst = HashMap<String, T>()
        private val icu = HashMap<String, Any>()

        /**
         * Adds a substitution argument.
         */
        fun subst(key: String, value: T) {
            subst[key] = value
        }

        /**
         * Adds a substitution argument based on the result of [Localizable.localize].
         */
        fun subst(key: String, item: Localizable<T>) {
            subst[key] = item.localize(this)
        }

        /**
         * Adds an ICU argument.
         */
        fun icu(key: String, value: Any) {
            icu[key] = value
        }

        /**
         * Adds a substitution argument from a list of [T] values.
        */
        fun list(key: String, values: Iterable<T>) {
            subst[key] = values.join(makeOne(LIST_SEPARATOR) ?: empty)
        }

        /**
         * Adds a substitution argument from a list of [T] values.
         */
        fun list(key: String, vararg values: T) = list(key, values.asIterable())

        /**
         * Creates the arguments.
         */
        fun build() = I18NArgs(subst, icu)
    }
}

/**
 * The core service for creating a translated message from a message key and arguments.
 *
 * Note that a single I18N instance has a fixed locale. To translate a message for a specific locale,
 * first create a new I18N instance using [withLocale].
 */
interface I18N<T> {
    /**
     * Creates a list of lines from a key and arguments if the message exists.
     */
    fun make(key: String, args: I18NArgs<T>): List<T>?

    /**
     * Creates a single element message from a key and arguments if the message exists.
     */
    fun makeOne(key: String, args: I18NArgs<T>, separator: T = empty) =
        make(key, args)?.join(separator)

    /**
     * Creates a list of lines from a key and arguments if the message exists.
     */
    fun make(key: String, args: I18NArgs.Scope<T>.() -> Unit = {}): List<T>? {
        return make(key, I18NArgs.Scope(this).apply(args).build())
    }

    /**
     * Creates a single element message from a key and arguments if the message exists.
     */
    fun makeOne(key: String, separator: T = empty, args: I18NArgs.Scope<T>.() -> Unit = {}) =
        make(key, args)?.join(separator)

    /**
     * Creates a list of lines from a key and arguments if it exists, otherwise a fallback message.
     */
    fun safe(key: String, args: I18NArgs<T>): List<T>

    /**
     * Creates a single element message from a key and arguments if it exists, otherwise a fallback message.
     */
    fun safeOne(key: String, args: I18NArgs<T>, separator: T = empty) =
        safe(key, args).join(separator)

    /**
     * Creates a list of lines from a key and arguments if it exists, otherwise a fallback message.
     */
    fun safe(key: String, args: I18NArgs.Scope<T>.() -> Unit = {}): List<T> {
        return safe(key, I18NArgs.Scope(this).apply(args).build())
    }

    /**
     * Creates a single element message from a key and arguments if it exists, otherwise a fallback message.
     */
    fun safeOne(key: String, separator: T = empty, args: I18NArgs.Scope<T>.() -> Unit = {}) =
        safe(key, args).join(separator)

    /**
     * Creates a new I18N service which uses the specified locale.
     */
    fun withLocale(locale: Locale): I18N<T>

    /**
     * Joins a collection of [T]s into one [T], with the specified separator.
     */
    fun Iterable<T>.join(separator: T): T

    /**
     * A [T] representing nothing.
     */
    val empty: T

    /**
     * A [T] representing a new line.
     */
    val newline: T
}

/**
 * An I18N which delegates operations to a backing service.
 */
interface ForwardingI18N<T> : I18N<T> {
    /**
     * The backing I18N used for delegating operations to.
     */
    val i18n: I18N<T>

    override fun make(key: String, args: I18NArgs<T>) = i18n.make(key, args)
    override fun safe(key: String, args: I18NArgs<T>) = i18n.safe(key, args)
    override fun Iterable<T>.join(separator: T) = i18n.run { this@join.join(separator) }
    override fun withLocale(locale: Locale) = i18n.withLocale(locale)
    override val empty get() = i18n.empty
    override val newline get() = i18n.newline
}
