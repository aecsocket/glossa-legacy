package com.github.aecsocket.glossa.configurate

import com.github.aecsocket.glossa.core.KeyValidationException
import com.github.aecsocket.glossa.core.Translation
import com.github.aecsocket.glossa.core.validate
import net.kyori.adventure.text.format.Style
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

object I18NLoader {
    private const val STYLES = "styles"
    private const val FORMATS = "formats"

    fun loadTranslations(node: ConfigurationNode, onError: (SerializationException) -> Unit): List<Translation.Root> {
        return ArrayList<Translation.Root>().apply {
            node.childrenMap().forEach { (_, child) ->
                try {
                    add(child.req())
                } catch (ex: SerializationException) {
                    onError(ex)
                }
            }
        }
    }

    fun loadStyles(node: ConfigurationNode, onError: (SerializationException) -> Unit): Map<String, Style> {
        return HashMap<String, Style>().apply {
            node.childrenMap().forEach { (rawKey, child) ->
                val key = try {
                    rawKey.toString().validate()
                } catch (ex: KeyValidationException) {
                    onError(SerializationException(child, String::class.java, "Invalid key"))
                    return@apply
                }
                try {
                    put(key, child.req())
                } catch (ex: SerializationException) {
                    onError(ex)
                }
            }
        }
    }

    data class Loaded(
        val translations: List<Translation.Root>,
        val styles: Map<String, Style>
    )

    fun loadAll(
        node: ConfigurationNode,
        onTlError: (SerializationException) -> Unit,
        onStyleError: (SerializationException) -> Unit
    ): Loaded {
        val styles = loadStyles(node.node(STYLES), onStyleError)

        val tlNode = node.copy().apply {
            removeChild(STYLES)
        }

        val translations = loadTranslations(tlNode, onTlError)

        return Loaded(
            translations,
            styles
        )
    }
}
