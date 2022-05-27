package com.github.aecsocket.glossa.configurate

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

inline fun <reified V> ConfigurationNode.force() = get(object : TypeToken<V>() {})
    ?: throw SerializationException(this, V::class.java, "A value is required for this field")
