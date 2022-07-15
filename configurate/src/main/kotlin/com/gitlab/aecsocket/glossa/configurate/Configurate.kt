package com.gitlab.aecsocket.glossa.configurate

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

internal inline fun <reified V> ConfigurationNode.force() = get(object : TypeToken<V>() {})
    ?: throw SerializationException(this, V::class.java, "A value is required for this field")
