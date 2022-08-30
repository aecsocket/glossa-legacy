package com.gitlab.aecsocket.glossa.adventure

import com.gitlab.aecsocket.glossa.core.AbstractI18N
import com.gitlab.aecsocket.glossa.core.I18NArg
import com.gitlab.aecsocket.glossa.core.I18NArgs
import com.ibm.icu.text.MessageFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.text.FieldPosition
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
        val argTemplates: Map<String, String>,
    )

    override fun make(key: String, args: I18NArgs<Component>): List<Component>? {
        return translation(key)?.let { data ->
            val resolverBuilder = TagResolver.builder()

            fun Component.styled(key: String) = applyFallbackStyle(data.argStyles[key] ?: Style.empty())

            // first build up the resolver to take non-ICU tags
            args.forEach { (key, arg) ->
                if (arg is I18NArg.Raw) {
                    resolverBuilder.tag(key, Tag.selfClosingInserting(arg.value.styled(key)))
                }
            }

            data.argStyles.forEach { (key, style) ->
                // if we haven't already added an inserting tag for this arg...
                if (!args.contains(key)) {
                    resolverBuilder.tag(key, Tag.styling { it.merge(style) })
                }
            }

            // we're ready to parse ICU templates
            val icuArgs = args
                .mapNotNull { (key, arg) -> (arg as? I18NArg.ICU)?.let { key to it.value } }
                .associate { it }
            val icuResolver = resolverBuilder.build()

            // start parsing them and adding them to the resolver
            args.forEach { (key, arg) ->
                if (arg is I18NArg.ICU) {
                    val content = data.argTemplates[key]?.let { template ->
                        val text = MessageFormat(template).build(icuArgs)
                        miniMessage.deserialize(text, icuResolver)
                    } ?: text("{$key}")
                    resolverBuilder.tag(key, Tag.selfClosingInserting(content.styled(key)))
                }
            }

            // we're done with all arguments, format the actual message now
            val resolver = resolverBuilder.build()
            data.lines.map { line ->
                val comp = miniMessage.deserialize(line, resolver)
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
            val style: String?,
            val argFormats: Map<String, ArgFormat>,
            val children: MutableMap<String, FormatNode>,
        ) {
            fun node(key: String) = children[key]

            fun node(path: Iterable<String>): FormatNode? {
                var cur = this
                path.forEach { cur = cur.node(it) ?: return null }
                return cur
            }

            fun node(vararg path: String) = node(path.asIterable())

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

                fun argTemplate(key: String, content: String, style: String? = null)

                fun node(key: String, builder: Scope.() -> Unit)
            }
        }

        private val styles = HashMap<String, Style>()
        private val formats = FormatNode(null, emptyMap(), HashMap())

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
                var formatNode = formats
                path.forEach {
                    formatNode = formatNode.node(it) ?: return@forEach
                    styles[formatNode.style]?.let { childStyle ->
                        style = style.merge(childStyle)
                    }
                }

                val lines = node.lines

                val argStyles: Map<String, Style> = formatNode.argFormats
                    .mapNotNull { (key, format) ->
                        when (format) {
                            is ArgFormat.Styling -> styles[format.style]
                            is ArgFormat.Templating -> format.style?.let { styles[it] }
                        }?.let { key to it }
                    }.associate { it }

                val argTemplates: Map<String, String> = formatNode.argFormats
                    .mapNotNull { (key, format) ->
                        (format as? ArgFormat.Templating)?.let { templating ->
                            key to templating.content
                        }
                    }.associate { it }

                Data(lines, style, argStyles, argTemplates)
            },
            locale,
            miniMessage,
        )
    }
}

fun ((MiniMessageI18N.Builder.FormatNode.Scope) -> Unit).build(): MiniMessageI18N.Builder.FormatNode {
    var mStyle: String? = null
    val mArgFormats = HashMap<String, MiniMessageI18N.Builder.ArgFormat>()
    val mChildren = HashMap<String, MiniMessageI18N.Builder.FormatNode>()
    invoke(object : MiniMessageI18N.Builder.FormatNode.Scope {
        override fun style(style: String) {
            mStyle = style
        }

        override fun argStyle(key: String, style: String) {
            mArgFormats[key] = MiniMessageI18N.Builder.ArgFormat.Styling(style)
        }

        override fun argTemplate(key: String, content: String, style: String?) {
            mArgFormats[key] = MiniMessageI18N.Builder.ArgFormat.Templating(content, style)
        }

        override fun node(key: String, builder: MiniMessageI18N.Builder.FormatNode.Scope.() -> Unit) {
            mChildren[key] = builder.build()
        }
    })
    return MiniMessageI18N.Builder.FormatNode(mStyle, mArgFormats, mChildren)
}

fun MessageFormat.build(args: Map<String, Any>): String {
    val res = StringBuffer()
    format(args, res, FieldPosition(0))
    return res.toString()
}
