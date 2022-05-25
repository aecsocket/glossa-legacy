package com.github.aecsocket.glossa.core

import java.util.*

open class I18NException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

const val THIS_ARG = "_"
const val I18N_SEPARATOR = "."

interface I18N<T> {
    val locale: Locale

    fun make(locale: Locale = this.locale, key: String, args: Argument.MapScope<T>.() -> Unit = {}): List<T>?

    fun make(key: String, args: Argument.MapScope<T>.() -> Unit = {}) = make(locale, key, args)

    fun safe(locale: Locale = this.locale, key: String, args: Argument.MapScope<T>.() -> Unit = {}): List<T>

    fun safe(key: String, args: Argument.MapScope<T>.() -> Unit = {}) = safe(locale, key, args)

    fun withLocale(locale: Locale): I18N<T> = object : I18N<T> {
        override val locale: Locale
            get() = locale

        override fun make(locale: Locale, key: String, args: Argument.MapScope<T>.() -> Unit) =
            this@I18N.make(locale, key, args)

        override fun safe(locale: Locale, key: String, args: Argument.MapScope<T>.() -> Unit) =
            this@I18N.safe(locale, key, args)
    }

    interface Builder<T> {
        fun build(): I18N<T>

        fun register(tl: Translation.Root)

        fun register(locale: Locale, content: Translation.Scope.() -> Unit) = register(Translation.buildRoot(locale, content))
    }
}

open class ForwardingI18N<T>(private val backing: I18N<T>) : I18N<T> {
    override val locale = backing.locale

    override fun make(locale: Locale, key: String, args: Argument.MapScope<T>.() -> Unit) =
        backing.make(locale, key, args)

    override fun safe(locale: Locale, key: String, args: Argument.MapScope<T>.() -> Unit) =
        backing.safe(locale, key, args)
}

interface Localizable<T> {
    fun localize(i18n: I18N<T>): List<T>
}

fun Iterable<String>.i18nPath() = joinToString(I18N_SEPARATOR)
