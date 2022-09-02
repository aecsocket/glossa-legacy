package com.gitlab.aecsocket.glossa.adventure

import com.gitlab.aecsocket.glossa.core.AbstractI18N
import com.gitlab.aecsocket.glossa.core.I18NArgs
import com.gitlab.aecsocket.glossa.core.build
import com.ibm.icu.text.MessageFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.*
import kotlin.collections.HashMap

class MiniMessageI18N(
    translations: Map<String, Map<Locale, Data>>,
    locale: Locale,
    val miniMessage: MiniMessage,
    currentLocale: Locale = locale,
) : AbstractI18N<Component, MiniMessageI18N.Data>(translations, locale, currentLocale), AdventureI18N {
    data class Token(
        val content: String,
        val style: Style? = null,
    )

    data class Data(
        val lines: List<String>,
        val style: Style,
        val argStyles: Map<String, Style>,
    )

    override fun make(key: String, args: I18NArgs<Component>): List<Component>? {
        return translation(key)?.let { data ->
            // build tag resolver for MM
            val resolverBuilder = TagResolver.builder()
            // · args passed through API get insertion tags
            args.subst.forEach { (key, subst) ->
                resolverBuilder.tag(key, Tag.selfClosingInserting(
                    data.argStyles[key]?.let { subst.applyFallbackStyle(it) }
                        ?: subst
                ))
            }
            // · arg styles passed through format get open/closing tags
            data.argStyles.forEach { (key, style) ->
                if (!args.subst.contains(key)) {
                    resolverBuilder.tag(key, Tag.styling { it.merge(style) })
                }
            }

            // build final message
            val resolver = resolverBuilder.build()
            data.lines.map { line ->
                // format ICU args
                val text = MessageFormat(line, currentLocale).build(args.icu)

                val comp = miniMessage.deserialize(text, resolver)
                comp.applyFallbackStyle(data.style)
            }
        }
    }

    override fun safe(key: String, args: I18NArgs<Component>): List<Component> {
        return make(key, args) ?: listOf(text(key))
    }

    override fun withLocale(locale: Locale) = MiniMessageI18N(translations, this.locale, miniMessage, locale)

    class Builder : AbstractI18N.Builder<Component, Data>() {
        sealed interface ArgFormat {
            data class Styling(val style: String) : ArgFormat

            data class Templating(val content: String, val style: String? = null) : ArgFormat
        }

        data class FormatNode(
            var style: String? = null,
            var argStyles: MutableMap<String, String> = HashMap(),
            val children: MutableMap<String, FormatNode> = HashMap(),
        ) {
            val size: Int get() = children.size

            fun sizeOfAll(): Int = size + children.values.sumOf { it.sizeOfAll() }

            fun node(key: String) = children[key]

            fun node(path: Iterable<String>): FormatNode? {
                var cur = this
                path.forEach { cur = cur.node(it) ?: return null }
                return cur
            }

            fun node(vararg path: String) = node(path.asIterable())

            fun forceNode(path: Iterable<String>): FormatNode {
                var cur = this
                path.forEach {
                    cur = cur.node(it) ?: FormatNode().also { node -> cur.children[it] = node }
                }
                return cur
            }

            fun forceNode(vararg path: String) = forceNode(path.asIterable())

            fun mergeFrom(other: FormatNode) {
                other.children.forEach { (key, node) ->
                    // if we already hold a node for this key, keep it (and merge its children)
                    // else we set the other's node for this key to ourselves
                    children[key]?.mergeFrom(node) ?: run {
                        children[key] = node
                    }
                }
            }

            interface Scope {
                fun style(style: String)

                fun argStyle(key: String, style: String)

                fun node(key: String, builder: Scope.() -> Unit)
            }
        }

        val styles = HashMap<String, Style>()
        val formats = FormatNode()

        fun style(key: String, style: Style) {
            styles[key] = style
        }

        fun style(key: String, builder: Style.Builder.() -> Unit) {
            styles[key] = Style.style(builder)
        }

        fun format(format: FormatNode) {
            formats.mergeFrom(format)
        }

        fun format(builder: FormatNode.Scope.() -> Unit) {
            formats.mergeFrom(builder.build())
        }

        fun build(locale: Locale, miniMessage: MiniMessage) = MiniMessageI18N(
            buildData { node, path ->
                // combine styles top-down to create the final style of this message
                var style = Style.empty()
                var format = formats
                path.forEach {
                    format = format.node(it) ?: return@forEach
                    styles[format.style]?.let { childStyle ->
                        style = style.merge(childStyle)
                    }
                }

                // if a style key is invalid, ignore it
                val argStyles = format.argStyles
                    .mapNotNull { (key, styleKey) ->
                        styles[styleKey]?.let { key to it }
                    }.associate { it }

                Data(node.lines, style, argStyles)
            },
            locale,
            miniMessage,
        )
    }
}

fun ((MiniMessageI18N.Builder.FormatNode.Scope) -> Unit).build(): MiniMessageI18N.Builder.FormatNode {
    var mStyle: String? = null
    val mArgStyles = HashMap<String, String>()
    val mChildren = HashMap<String, MiniMessageI18N.Builder.FormatNode>()
    invoke(object : MiniMessageI18N.Builder.FormatNode.Scope {
        override fun style(style: String) {
            mStyle = style
        }

        override fun argStyle(key: String, style: String) {
            mArgStyles[key] = style
        }

        override fun node(key: String, builder: MiniMessageI18N.Builder.FormatNode.Scope.() -> Unit) {
            mChildren[key] = builder.build()
        }
    })
    return MiniMessageI18N.Builder.FormatNode(mStyle, mArgStyles, mChildren)
}
