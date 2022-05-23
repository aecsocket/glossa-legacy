package com.github.aecsocket.glossa.core.extension

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

inline fun <reified T> typeToken() = object : TypeToken<T>() {}

fun <V> ConfigurationNode.req(type: TypeToken<V>) = get(type)
    ?: throw SerializationException(this, type.type, "")

inline fun <reified V> ConfigurationNode.req() = req(typeToken<V>())
