package com.github.aecsocket.glossa.core

import java.util.*
import kotlin.collections.HashMap

abstract class TemplatingI18N<E>(
    locale: Locale = Locale.ROOT
) : AbstractI18N<List<E>, Args>(locale) {
    private val cache = HashMap<Locale, MutableMap<String, Templating.Node?>>()

    override fun get(locale: Locale, key: String, args: Args): List<E>? {
        return cache.computeIfAbsent(locale) { HashMap() }.computeIfAbsent(key) {
            translation(locale, key)?.let { Templating.parse(it) }
        }?.let { node ->
            Templating.format(locale, node, args).lines().map { toE(it) }
        }
    }

    operator fun get(locale: Locale, key: String) = get(locale, key, Args.EMPTY)

    operator fun get(key: String) = get(key, Args.EMPTY)

    fun safe(locale: Locale, key: String) = safe(locale, key, Args.EMPTY)

    fun safe(key: String) = safe(key, Args.EMPTY)

    protected abstract fun toE(line: List<Templating.FormatToken>): E
}
