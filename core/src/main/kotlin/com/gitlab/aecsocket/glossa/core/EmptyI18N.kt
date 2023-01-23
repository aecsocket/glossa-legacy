package com.gitlab.aecsocket.glossa.core

import java.util.*

/**
 * An I18N which returns no values.
 */
class EmptyI18N<T>(override val empty: T) : I18N<T> {
    override fun make(key: String, args: I18NArgs<T>) = null
    override fun safe(key: String, args: I18NArgs<T>) = emptyList<T>()
    override fun Iterable<T>.join(separator: T) = empty
    override fun withLocale(locale: Locale) = this
    override val newline get() = empty
}
