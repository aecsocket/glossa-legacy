package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.text.FieldPosition
import java.util.Locale

open class I18NException(key: String, message: String) :
    RuntimeException("($key) $message")

interface I18N<T, A> {
    var locale: Locale

    operator fun get(locale: Locale, key: String, args: A): T?

    operator fun get(key: String, args: A) =
        get(locale, key, args)


    fun safe(locale: Locale, key: String, args: A): T

    fun safe(key: String, args: A) =
        safe(locale, key, args)
}

class Translation(val locale: Locale, m: Map<out String, String>) : HashMap<String, String>(m) {
    constructor(locale: Locale) : this(locale, emptyMap())

    fun copy() = Translation(locale, this)
}

interface MutableI18N<T, A> : I18N<T, A> {
    fun register(tl: Translation)

    fun register(locale: Locale, vararg tl: Pair<String, String>) =
        register(Translation(locale, tl.associate { it }))

    fun clear()
}

abstract class AbstractI18N<T, A>(
    override var locale: Locale = Locale.ROOT
) : MutableI18N<T, A> {
    protected val translations = HashMap<Locale, Translation>()

    override fun register(tl: Translation) {
        translations[tl.locale]?.putAll(tl) ?: run { translations[tl.locale] = tl.copy() }
    }

    protected fun translation(locale: Locale, key: String) =
        translations[locale]?.get(key) ?: translations[this.locale]?.get(key)

    override fun clear() {
        translations.clear()
    }
}

fun MessageFormat.asString(args: Map<String, Any?>): String = StringBuffer().apply {
    this@asString.format(args, this, FieldPosition(0))
}.toString()
