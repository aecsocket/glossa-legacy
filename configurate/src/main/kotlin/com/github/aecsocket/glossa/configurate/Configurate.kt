package com.github.aecsocket.glossa.configurate

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import kotlin.reflect.KClass

inline fun <reified T> typeToken() = object : TypeToken<T>() {}

fun <V> ConfigurationNode.req(type: TypeToken<V>) = get(type)
    ?: throw SerializationException(this, type.type, "A value is required for this field")

inline fun <reified V> ConfigurationNode.req() = req(typeToken<V>())

fun <T : Any> TypeSerializerCollection.Builder.register(
    type: KClass<T>,
    serializer: TypeSerializer<in T>
) = register(type.java, serializer)
