package com.github.aecsocket.glossa.core

import java.util.Locale

interface I18N<T> {
    var locale: Locale

    operator fun get(locale: Locale, key: String, args: Map<String, () -> T>): T?

    operator fun get(locale: Locale, key: String, vararg args: Pair<String, () -> T>) =
        get(locale, key, args.associate { it })

    operator fun get(key: String, args: Map<String, () -> T>) =
        get(locale, key, args)

    operator fun get(key: String, vararg args: Pair<String, () -> T>) =
        get(key, args.associate { it })


    fun safe(locale: Locale, key: String, args: Map<String, () -> T>): T

    fun safe(locale: Locale, key: String, vararg args: Pair<String, () -> T>) =
        safe(locale, key, args.associate { it })

    fun safe(key: String, args: Map<String, () -> T>) =
        safe(locale, key, args)

    fun safe(key: String, vararg args: Pair<String, () -> T>) =
        safe(locale, key, args.associate { it })
}

class Translation(val locale: Locale, m: Map<out String, List<String>>) : HashMap<String, List<String>>(m) {
    constructor(locale: Locale) : this(locale, emptyMap())

    fun copy() = Translation(locale, this)
}

interface MutableI18N<T> : I18N<T> {
    fun register(tl: Translation)

    fun clear()
}

abstract class AbstractI18N<T>(
    override var locale: Locale = Locale.ROOT
) : MutableI18N<T> {
    private val cache = HashMap<Locale, MutableMap<String, T>>()
    protected val translations = HashMap<Locale, Translation>()

    override fun get(locale: Locale, key: String, args: Map<String, () -> T>): T? {
        val caches = args.isEmpty()
        if (caches) {
            cache[locale]?.let { cache -> cache[key]?.let { return it } }
        }

        return doGet(locale, key, args)?.also {
            if (caches) {
                cache.computeIfAbsent(locale) { HashMap() }[key] = it
            }
        }
    }

    protected abstract fun doGet(locale: Locale, key: String, args: Map<String, () -> T>): T?

    override fun register(tl: Translation) {
        translations[tl.locale]?.putAll(tl) ?: run { translations[tl.locale] = tl.copy() }
    }

    override fun clear() {
        cache.clear()
    }
}
