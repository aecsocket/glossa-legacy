package com.github.aecsocket.glossa.api

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.HashMap

/**
 * A string key-value map, additionally storing what locale this translation
 * is effective for.
 *
 * @property locale the locale this translation is for.
 * @param m the underlying map.
 */
class Translation(val locale: Locale, m: Map<out String, String>) : HashMap<String, String>(m) {
    constructor(locale: Locale) : this(locale, emptyMap())

    /**
     * Deeply copies this translation.
     * @return the copy.
     */
    fun copy() = Translation(locale, this)

    object Serializer : TypeSerializer<Translation> {
        @JvmStatic val LOCALE = "__locale__"

        override fun serialize(type: Type, obj: Translation?, node: ConfigurationNode) {
            if (obj == null) node.set(null)
            else {
                for ((key, value) in obj) {
                    val lines = value.split('\n')
                    if (lines.size == 1) {
                        node.node(key).set(value)
                    } else {
                        node.node(key).set(lines)
                    }
                }
                node.node(LOCALE).set(obj.locale)
            }
        }

        override fun deserialize(type: Type, node: ConfigurationNode) = Translation(
            Locale.forLanguageTag(node.node(LOCALE).req(String::class)),
            node.childrenMap().map { (key, value) ->
                ""+key to if (value.isList)
                    // cannot use List<String>:
                    //   ? extends java.lang.String: No applicable type serializer for type
                    value.req(typeToken<MutableList<String>>()).joinToString("\n")
                else
                    value.req(String::class)
            }.associate { it }
        )
    }
}
