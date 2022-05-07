package com.github.aecsocket.glossa.api

import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.lang.reflect.Type
import kotlin.reflect.KClass

fun <V : Any> ConfigurationNode.req(type: TypeToken<V>) = get(type)
    ?: throw SerializationException(this, type.type, "A value is required for this field")

fun <V : Any> ConfigurationNode.req(type: KClass<V>) = get(type.java)
    ?: throw SerializationException(this, type.java, "A value is required for this field")

fun ConfigurationNode.req(type: Type) = get(type)
    ?: throw SerializationException(this, type, "A value is required for this field")

fun <V : Any> ConfigurationNode.getList(type: KClass<V>) = getList(type.java)

fun <T : Any> TypeSerializerCollection.Builder.register(type: KClass<T>, serializer: TypeSerializer<in T>): TypeSerializerCollection.Builder =
    register(type.java, serializer)

inline fun <reified T> typeToken() = object : TypeToken<T>() {}
