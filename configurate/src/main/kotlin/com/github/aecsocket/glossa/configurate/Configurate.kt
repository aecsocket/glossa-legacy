package com.github.aecsocket.glossa.configurate

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

inline fun <reified V> ConfigurationNode.force() = get(object : TypeToken<V>() {})
    ?: throw SerializationException(this, V::class.java, "A value is required for this field")

fun ConfigurationNode.forceList(type: Type) = if (isList) childrenList()
    else throw SerializationException(this, type, "Field must be expressed as list")

fun ConfigurationNode.recursive(
    path: List<String> = emptyList(),
    action: (List<String>, ConfigurationNode) -> Unit
) {
    childrenMap().forEach { (key, child) ->
        val newPath = path + key.toString()
        if (child.isMap) {
            child.recursive(newPath, action)
        } else {
            action(newPath, child)
        }
    }
}
