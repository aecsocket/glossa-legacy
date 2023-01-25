package io.gitlab.aecsocket.glossa.adventure

import com.ibm.icu.text.MessageFormat
import io.gitlab.aecsocket.glossa.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.*
import kotlin.collections.HashMap

class KeyStyleNode(val style: String? = null) {
    val children: MutableMap<String, KeyStyleNode> = HashMap()

    fun node(key: String) = children[key]

    fun node(path: Iterable<String>): KeyStyleNode? {
        var cur = this
        path.forEach { cur = cur.node(it) ?: return null }
        return cur
    }

    fun node(key: String, child: KeyStyleNode) {
        children[key] = child
    }

    fun mergeFrom(other: KeyStyleNode) {
        other.children.forEach { (key, node) ->
            children[key]?.mergeFrom(node) ?: run {
                children[key] = node
            }
        }
    }

    fun with(builder: Scope.() -> Unit): KeyStyleNode {
        val scope = object : Scope {
            override fun node(key: String, style: String?, builder: Scope.() -> Unit) {
                children[key] = KeyStyleNode(style).with(builder)
            }
        }

        builder(scope)
        return this
    }

    interface Scope {
        fun node(key: String, style: String? = null, builder: Scope.() -> Unit = {})
    }
}

class MiniMessageI18N(
    translations: Map<String, Map<Locale, TranslationData>>,
    locale: Locale,
    private val miniMessage: MiniMessage,
    private val substitutions: Map<String, Component>,
    private val styles: Map<String, Style>,
    currentLocale: Locale = locale,
) : AbstractI18N<Component, MiniMessageI18N.TranslationData>(translations, locale, currentLocale), AdventureI18N {
    data class TranslationData(
        val lines: List<String>,
        val baseStyle: Style
    )

    override fun make(key: String, args: I18NArgs<Component>): List<Component>? {
        val data = translation(key) ?: return null

        val tagResolver = TagResolver.builder().apply {
            // lowest priority

            // substitutions get insertion tags
            substitutions.forEach { (key, value) ->
                tag(key, Tag.selfClosingInserting(value))
            }
            // styles get open/closing tags
            styles.forEach { (key, style) ->
                tag(key, Tag.styling { it.merge(style) })
            }
            // args passed through API get insertion tags
            args.subst.forEach { (key, value) ->
                tag(key, Tag.selfClosingInserting(value))
            }

            // highest priority
        }.build()

        return data.lines.map { line ->
            val text = MessageFormat(line, locale).build(args.icu)
            val component = miniMessage.deserialize(text, tagResolver)
            component.applyFallbackStyle(data.baseStyle)
        }
    }

    override fun safe(key: String, args: I18NArgs<Component>): List<Component> {
        return make(key, args) ?: listOf(text(key))
    }

    override fun withLocale(locale: Locale) = MiniMessageI18N(
        translationData, baseLocale,
        miniMessage, substitutions, styles,
        locale
    )

    class Builder : AbstractI18N.Builder<Component, TranslationData>() {
        val substitutions: MutableMap<String, String> = HashMap()
        val styles: MutableMap<String, Style> = HashMap()
        val keyStyle = KeyStyleNode()

        fun build(locale: Locale, miniMessage: MiniMessage): MiniMessageI18N {
            val translationData = HashMap<String, MutableMap<Locale, TranslationData>>()

            fun makeTranslationData(
                locale: Locale,
                node: TranslationNode,
                keyStyle: KeyStyleNode?,
                style: Style,
                path: List<String>
            ) {
                val currentStyle = keyStyle?.let {
                    val childStyle = styles[it.style] ?: return@let null
                    style.merge(childStyle)
                } ?: style

                if (node is TranslationNode.Value) {
                    node.lines.forEachIndexed { idx, line ->
                        try {
                            MessageFormat(line).build()
                        } catch (ex: IllegalArgumentException) {
                            throw I18NBuildException("Invalid ICU format at $path line ${idx+1}", ex)
                        }
                    }

                    val key = path.joinToString(PATH_SEPARATOR)
                    val forKey = translationData.computeIfAbsent(key) { HashMap() }
                    forKey[locale] = TranslationData(node.lines, currentStyle)
                }

                node.children.forEach { (key, child) ->
                    makeTranslationData(locale, child, keyStyle?.node(key), currentStyle, path + key)
                }
            }

            translations.forEach { root ->
                makeTranslationData(root.locale, root, keyStyle, Style.empty(), emptyList())
            }

            val compSubstitutions = substitutions.map { (key, value) ->
                key to miniMessage.deserialize(value)
            }.associate { it }

            return MiniMessageI18N(translationData, locale, miniMessage, compSubstitutions, styles)
        }
    }
}
