package com.github.aecsocket.glossa.configurate

import com.github.aecsocket.glossa.core.KeyValidationException
import com.github.aecsocket.glossa.core.Translation
import com.github.aecsocket.glossa.core.validate
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.lang.reflect.Type
import java.util.Locale

private const val ROOT = "root"

private fun locale(tag: String): Locale = if (tag == ROOT) Locale.ROOT else Locale.forLanguageTag(tag)

object LocaleSerializer : TypeSerializer<Locale> {
    override fun serialize(type: Type, obj: Locale?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            node.set(if (obj == Locale.ROOT) ROOT else obj.toLanguageTag())
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode) = locale(node.req())
}

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
            node.childrenMap().forEach { (rawKey, child) ->
                val key = try {
                    rawKey.toString().validate()
                } catch (ex: KeyValidationException) {
                    throw SerializationException(node, type, "Invalid key", ex)
                }

                res[key] = if (child.isMap) Translation.Section(deserialize0(child))
                else Translation.Value(
                    // mutable, else it's treated as `? extends String`
                    if (child.isList) child.req<MutableList<String>>().joinToString("\n")
                    else child.req()
                )
            }
            return res
        }

        return Translation.Root(
            locale(node.key().toString()),
            deserialize0(node)
        )
    }
}

object I18NSerializers {
    val ALL = TypeSerializerCollection.builder()
        .register(Locale::class, LocaleSerializer)
        .register(Translation.Root::class, TranslationSerializer)
        .registerAll(ConfigurateComponentSerializer.configurate().serializers())
        .build()
}
