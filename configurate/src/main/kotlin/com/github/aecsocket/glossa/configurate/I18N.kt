package com.github.aecsocket.glossa.configurate

import com.github.aecsocket.glossa.adventure.I18NFormat
import com.github.aecsocket.glossa.core.I18N_SEPARATOR
import com.github.aecsocket.glossa.core.Translation
import net.kyori.adventure.text.format.Style
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.SerializationException

object I18NLoader {
    private const val STYLES = "styles"
    private const val FORMATS = "formats"

    fun loadTranslations(node: ConfigurationNode): List<Translation.Root> {
        return ArrayList<Translation.Root>().apply {
            node.childrenMap().forEach { (_, child) ->
                add(child.get() ?: throw SerializationException(child, Translation.Root::class.java, null, null))
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
        return node.childrenMap()
            .map { (key, child) -> key.toString().split(I18N_SEPARATOR) to child.force<I18NFormat>() }
            .associate { it }
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
