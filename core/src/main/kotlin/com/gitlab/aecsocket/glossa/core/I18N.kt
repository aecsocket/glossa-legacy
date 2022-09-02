package com.gitlab.aecsocket.glossa.core

import java.util.Locale

private const val LIST_SEPARATOR = "list_separator"

data class I18NArgs<T>(
    val subst: Map<String, T>,
    val icu: Map<String, Any>,
) {
    operator fun plus(other: I18NArgs<T>) = I18NArgs(
        subst + other.subst,
        icu + other.icu
    )

    class Scope<T>(override val i18n: I18N<T>) : ForwardingI18N<T> {
        private val subst = HashMap<String, T>()
        private val icu = HashMap<String, Any>()

        fun subst(key: String, value: T) {
            subst[key] = value
        }

        fun subst(key: String, item: Localizable<T>) {
            subst[key] = item.localize(this)
        }

        fun icu(key: String, value: Any) {
            icu[key] = value
        }

        fun list(key: String, values: Iterable<T>) {
            subst[key] = values.join(makeLine(LIST_SEPARATOR) ?: empty)
        }

        fun list(key: String, vararg values: T) = list(key, values.asIterable())

        fun build() = I18NArgs(subst, icu)
    }
}

interface I18N<T> {
    fun make(key: String, args: I18NArgs<T>): List<T>?

    fun makeLine(key: String, args: I18NArgs<T>) =
        make(key, args)?.join(empty)

    fun make(key: String, args: I18NArgs.Scope<T>.() -> Unit = {}): List<T>? {
        return make(key, I18NArgs.Scope(this).apply(args).build())
    }

    fun makeLine(key: String, args: I18NArgs.Scope<T>.() -> Unit = {}) =
        make(key, args)?.join(empty)

    fun safe(key: String, args: I18NArgs<T>): List<T>

    fun safeLine(key: String, args: I18NArgs<T>) =
        safe(key, args).join(empty)

    fun safe(key: String, args: I18NArgs.Scope<T>.() -> Unit = {}): List<T> {
        return safe(key, I18NArgs.Scope(this).apply(args).build())
    }

    fun safeLine(key: String, args: I18NArgs.Scope<T>.() -> Unit = {}) =
        safe(key, args).join(empty)

    fun withLocale(locale: Locale): I18N<T>

    fun Iterable<T>.join(separator: T): T

    val empty: T

    val newline: T
}

interface ForwardingI18N<T> : I18N<T> {
    val i18n: I18N<T>

    override fun make(key: String, args: I18NArgs<T>) = i18n.make(key, args)
    override fun safe(key: String, args: I18NArgs<T>) = i18n.safe(key, args)
    override fun Iterable<T>.join(separator: T) = i18n.run { this@join.join(separator) }
    override fun withLocale(locale: Locale) = i18n.withLocale(locale)
    override val empty get() = i18n.empty
    override val newline get() = i18n.newline
}
