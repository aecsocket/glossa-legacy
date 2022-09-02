package com.gitlab.aecsocket.glossa.core

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val PATH_SEPARATOR = "."

abstract class AbstractI18N<T, D>(
    val translations: Map<String, Map<Locale, D>>,
    val locale: Locale,
    val currentLocale: Locale = locale,
) : I18N<T> {
    protected fun translation(key: String): D? {
        return translations[key]?.let {
            it[currentLocale] ?: it[locale] ?: it[Locale.ROOT]
        }
    }

    abstract class Builder<T, D> {
        protected val translations = ArrayList<TranslationNode.Root>()

        fun translation(node: TranslationNode.Root) {
            translations.add(node)
        }

        fun translation(locale: Locale, builder: TranslationNode.Scope.() -> Unit) {
            translations.add(TranslationNode.Root(locale, builder.build()))
        }

        protected fun buildData(
            dataFactory: (TranslationNode.Value, List<String>) -> D
        ): Map<String, Map<Locale, D>> {
            val res = HashMap<String, MutableMap<Locale, D>>()
            translations.forEach { tl ->
                tl.visit { node, path ->
                    if (node is TranslationNode.Value) {
                        res.computeIfAbsent(path.joinToString(PATH_SEPARATOR)) {
                            HashMap()
                        }[tl.locale] = dataFactory(node, path)
                    }
                }
            }
            return res
        }
    }
}
