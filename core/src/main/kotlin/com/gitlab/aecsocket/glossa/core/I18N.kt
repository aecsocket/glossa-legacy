package com.gitlab.aecsocket.glossa.core

import java.util.Locale

private const val LIST_SEPARATOR = "list_separator"

sealed interface I18NArg<T> {
    data class ICU<T>(val value: Any) : I18NArg<T>

    data class Raw<T>(val value: T) : I18NArg<T>
}

typealias I18NArgs<T> = Map<String, I18NArg<T>>

interface I18N<T> {
    fun make(key: String, args: I18NArgs<T>): List<T>?

    fun make(key: String, args: I18NScope<T>.() -> Unit = {}): List<T>? {
        return make(key, I18NScope(this).apply(args).build())
    }

    fun safe(key: String, args: I18NArgs<T>): List<T>

    fun safe(key: String, args: I18NScope<T>.() -> Unit = {}): List<T> {
        return safe(key, I18NScope(this).apply(args).build())
    }

    fun withLocale(locale: Locale): I18N<T>

    fun Iterable<T>.join(separator: T): T

    val empty: T

    val newline: T
}

open class ForwardingI18N<T>(private val backing: I18N<T>) : I18N<T> {
    override fun make(key: String, args: I18NArgs<T>) = backing.make(key, args)
    override fun safe(key: String, args: I18NArgs<T>) = backing.safe(key, args)
    override fun Iterable<T>.join(separator: T) = backing.run { this@join.join(separator) }
    override fun withLocale(locale: Locale) = backing.withLocale(locale)
    override val empty get() = backing.empty
    override val newline get() = backing.newline
}


class I18NScope<T>(backing: I18N<T>) : ForwardingI18N<T>(backing) {
    private val args = HashMap<String, I18NArg<T>>()

    fun icu(key: String, value: Any) {
        args[key] = I18NArg.ICU(value)
    }

    fun raw(key: String, value: T) {
        args[key] = I18NArg.Raw(value)
    }

    fun list(key: String, values: Iterable<T>) {
        args[key] = I18NArg.Raw(values.join(make(LIST_SEPARATOR)?.join(newline) ?: empty))
    }

    fun list(key: String, vararg values: T) = list(key, values.asIterable())

    fun build() = args
}
