package com.gitlab.aecsocket.glossa.configurate

import com.gitlab.aecsocket.glossa.adventure.I18NFormat
import com.gitlab.aecsocket.glossa.core.I18N_SEPARATOR
import com.gitlab.aecsocket.glossa.core.KeyValidationException
import com.gitlab.aecsocket.glossa.core.Keys
import com.gitlab.aecsocket.glossa.core.Translation
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.HashMap

object TranslationSerializer : TypeSerializer<Translation.Root> {
    private const val ROOT = "root"

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

        val key = node.key().toString()
        return Translation.Root(
            if (key == ROOT) Locale.ROOT else Locale.forLanguageTag(key),
            deserialize0(node)
        )
    }
}

object I18NFormatSerializer : TypeSerializer<I18NFormat> {
    private const val STYLE = "style"
    private const val ARGS = "args"

    override fun serialize(type: Type, obj: I18NFormat?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            node.appendListNode().set(obj.style)
            node.appendListNode().set(obj.args)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): I18NFormat {
        fun args(node: ConfigurationNode) = node.childrenMap().map { (key, child) ->
            key.toString().split(I18N_SEPARATOR) to child.force<String>()
        }.associate { it }

        return if (node.isList) {
            I18NFormat(
                node.node(0).get(),
                args(node.node(1))
            )
        } else if (node.isMap) {
            I18NFormat(null, args(node))
        } else {
            I18NFormat(node.force())
        }
    }
}

object I18NSerializers {
    /**
     * All type serializers that are required for loading [ConfigurationNode]s.
     * This collection includes [ConfigurateComponentSerializer] serializers.
     */
    val ALL = TypeSerializerCollection.builder()
        .register(Translation.Root::class.java, TranslationSerializer)
        .register(I18NFormat::class.java, I18NFormatSerializer)
        .registerAll(ConfigurateComponentSerializer.configurate().serializers())
        .build()

    internal fun key(node: ConfigurationNode) = try {
        Keys.validate(node.key().toString())
    } catch (ex: KeyValidationException) {
        throw SerializationException(node, String::class.java, "Invalid key", ex)
    }
}
