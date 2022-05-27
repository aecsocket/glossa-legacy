package com.github.aecsocket.glossa.configurate

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.extension.register
import com.github.aecsocket.alexandria.core.serializer.LocaleSerializer
import com.github.aecsocket.glossa.adventure.I18NFormat
import com.github.aecsocket.glossa.core.KeyValidationException
import com.github.aecsocket.glossa.core.Translation
import com.github.aecsocket.glossa.core.validate
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.lang.reflect.Type

object TranslationSerializer : TypeSerializer<Translation.Root> {
    override fun serialize(type: Type, obj: Translation.Root?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            obj.walk { path, tl ->
                if (tl is Translation.Value) {
                    node.node(path).set(tl.value)
                }
            }
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): Translation.Root {
        fun deserialize0(node: ConfigurationNode): Map<String, Translation> {
            val res = HashMap<String, Translation>()
            node.childrenMap().forEach { (_, child) ->
                val key = I18NSerializers.key(child)

                res[key] = if (child.isMap) Translation.Section(deserialize0(child))
                else Translation.Value(
                    // mutable, else it's treated as `? extends String`
                    if (child.isList) child.force<MutableList<String>>().joinToString("\n")
                    else child.force()
                )
            }
            return res
        }

        return Translation.Root(
            LocaleSerializer.fromString(node.key().toString()),
            deserialize0(node)
        )
    }
}

object I18NFormatSerializer : TypeSerializer<I18NFormat> {
    override fun serialize(type: Type, obj: I18NFormat?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            node.appendListNode().set(obj.style)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): I18NFormat {
        if (!node.isList)
            throw SerializationException(node, type, "Format must be list")
        return I18NFormat(
            node.node(0).force()
        )
    }
}

object I18NSerializers {
    val ALL = TypeSerializerCollection.builder()
        .register(Translation.Root::class, TranslationSerializer)
        .register(I18NFormat::class.java, I18NFormatSerializer)
        .registerAll(ConfigurateComponentSerializer.configurate().serializers())
        .build()

    fun key(node: ConfigurationNode) = try {
        node.key().toString().validate()
    } catch (ex: KeyValidationException) {
        throw SerializationException(node, String::class.java, "Invalid key", ex)
    }
}
