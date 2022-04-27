package com.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.text.FieldPosition
import java.util.Locale

open class I18NException(key: String, message: String) :
    RuntimeException("($key) $message")

interface I18N<T, A> {
    var locale: Locale

    fun asContext(locale: Locale) = I18NContext(this, locale)

    operator fun get(locale: Locale, key: String, args: Map<String, (I18NContext<T, A>) -> A>): T?

    operator fun get(locale: Locale, key: String, vararg args: Pair<String, (I18NContext<T, A>) -> A>) =
        get(locale, key, args.associate { it })

    operator fun get(key: String, args: Map<String, (I18NContext<T, A>) -> A>) =
        get(locale, key, args)

    operator fun get(key: String, vararg args: Pair<String, (I18NContext<T, A>) -> A>) =
        get(key, args.associate { it })


    fun safe(locale: Locale, key: String, args: Map<String, (I18NContext<T, A>) -> A>): T

    fun safe(locale: Locale, key: String, vararg args: Pair<String, (I18NContext<T, A>) -> A>) =
        safe(locale, key, args.associate { it })

    fun safe(key: String, args: Map<String, (I18NContext<T, A>) -> A>) =
        safe(locale, key, args)

    fun safe(key: String, vararg args: Pair<String, (I18NContext<T, A>) -> A>) =
        safe(locale, key, args.associate { it })
}

data class I18NContext<T, A>(val i18n: I18N<T, A>, val locale: Locale) {
    operator fun get(key: String, args: Map<String, (I18NContext<T, A>) -> A>) =
        i18n[locale, key, args]

    operator fun get(key: String, vararg args: Pair<String, (I18NContext<T, A>) -> A>) =
        i18n.get(locale, key, *args)

    fun safe(key: String, args: Map<String, (I18NContext<T, A>) -> A>) =
        i18n.safe(locale, key, args)

    fun safe(key: String, vararg args: Pair<String, (I18NContext<T, A>) -> A>) =
        i18n.safe(locale, key, *args)
}

class Translation(val locale: Locale, m: Map<out String, List<String>>) : HashMap<String, List<String>>(m) {
    constructor(locale: Locale) : this(locale, emptyMap())

    fun copy() = Translation(locale, this)
}

interface MutableI18N<T, A> : I18N<T, A> {
    fun register(tl: Translation)

    fun clear()
}

abstract class AbstractI18N<T, A>(
    override var locale: Locale = Locale.ROOT
) : MutableI18N<T, A> {
    private val cache = HashMap<Locale, MutableMap<String, T>>()
    protected val translations = HashMap<Locale, Translation>()

    override fun get(locale: Locale, key: String, args: Map<String, (I18NContext<T, A>) -> A>): T? {
        val caches = args.isEmpty()
        if (caches) {
            cache[locale]?.let { cache -> cache[key]?.let { return it } }
        }

        return (doGet(locale, key, args) ?: doGet(this.locale, key, args))?.also {
            if (caches) {
                cache.computeIfAbsent(locale) { HashMap() }[key] = it
            }
        }
    }

    protected abstract fun doGet(locale: Locale, key: String, args: Map<String, (I18NContext<T, A>) -> A>): T?

    override fun register(tl: Translation) {
        translations[tl.locale]?.putAll(tl) ?: run { translations[tl.locale] = tl.copy() }
    }

    override fun clear() {
        cache.clear()
    }
}
class ArgCache<T, A>(private val args: Map<String, (I18NContext<T, A>) -> A>) {
    private val cache = HashMap<String, A?>()

    operator fun get(ctx: I18NContext<T, A>, arg: String) =
        cache.computeIfAbsent(arg) { args[it]?.invoke(ctx) }
}

fun MessageFormat.asString(args: Map<String, Any?>): String = StringBuffer().apply {
    this@asString.format(args, this, FieldPosition(0))
}.toString()

fun MessageFormat.asString(vararg args: Pair<String, Any?>): String = asString(args.associate { it })
