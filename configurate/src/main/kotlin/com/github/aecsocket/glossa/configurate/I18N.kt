package com.github.aecsocket.glossa.configurate

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.glossa.adventure.I18NFormat
import com.github.aecsocket.glossa.core.Translation
import net.kyori.adventure.text.format.Style
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

object I18NLoader {
    private const val STYLES = "styles"
    private const val FORMATS = "formats"

    fun loadTranslations(node: ConfigurationNode): List<Translation.Root> {
        return ArrayList<Translation.Root>().apply {
            node.childrenMap().forEach { (_, child) ->
                add(child.force())
            }
        }
    }

    fun loadStyles(node: ConfigurationNode): Map<String, Style> {
        return HashMap<String, Style>().apply {
            node.childrenMap().forEach { (_, child) ->
                put(I18NSerializers.key(child), child.force())
            }
        }
    }

    fun loadFormats(node: ConfigurationNode): Map<List<String>, I18NFormat> {
        val res = HashMap<List<String>, I18NFormat>()
        fun loadFormats0(node: ConfigurationNode, path: List<String> = emptyList()) {
            node.childrenMap().forEach { (_, child) ->
                val newPath = path + I18NSerializers.key(child)
                if (child.isList) {
                    res[newPath] = child.force()
                } else if (child.isMap) {
                    loadFormats0(child, newPath)
                } else
                    throw SerializationException(child, I18NFormat::class.java, "Format must be list or map")
            }
        }
        loadFormats0(node)
        return res
    }

    data class Loaded(
        val translations: List<Translation.Root>,
        val styles: Map<String, Style>,
        val formats: Map<List<String>, I18NFormat>
    )

    fun loadAll(
        node: ConfigurationNode
    ): Loaded {
        val styles = try {
            loadStyles(node.node(STYLES))
        } catch (ex: SerializationException) {
            throw SerializationException(node, Style::class.java, "Could not load styles", ex)
        }
        val formats = try {
            loadFormats(node.node(FORMATS))
        } catch (ex: SerializationException) {
            throw SerializationException(node, I18NFormat::class.java, "Could not load formats", ex)
        }

        val tlNode = node.copy().apply {
            removeChild(STYLES)
            removeChild(FORMATS)
        }

        val translations = try {
            loadTranslations(tlNode)
        } catch (ex: SerializationException) {
            throw SerializationException(node, Translation.Root::class.java, "Could not load translations", ex)
        }

        return Loaded(
            translations,
            styles,
            formats
        )
    }
}
