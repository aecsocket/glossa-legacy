package io.gitlab.aecsocket.glossa.core

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * The separator for paths in message keys.
 */
const val PATH_SEPARATOR = "."

abstract class AbstractI18N<T, D>(
    val translationData: Map<String, Map<Locale, D>>,
    val baseLocale: Locale,
    val locale: Locale = baseLocale,
) : I18N<T> {
    /**
     * Gets translation data by its key. Uses this priority of locales:
     * * [locale]
     * * [baseLocale]
     * * [Locale.ROOT]
     */
    protected fun translation(key: String): D? {
        val forKey = translationData[key] ?: return null
        return forKey[locale] ?: forKey[baseLocale] ?: forKey[Locale.ROOT]
    }

    abstract class Builder<T, D> {
        val translations: MutableList<TranslationNode.Root> = ArrayList()
    }
}

class I18NBuildException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
