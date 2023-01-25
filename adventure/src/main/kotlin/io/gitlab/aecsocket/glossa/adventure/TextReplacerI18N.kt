package io.gitlab.aecsocket.glossa.adventure

import io.gitlab.aecsocket.glossa.core.ForwardingI18N
import io.gitlab.aecsocket.glossa.core.I18N
import io.gitlab.aecsocket.glossa.core.I18NArgs
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import java.util.*

class TextReplacerI18N(
    backing: I18N<Component>,
    private val replacements: List<TextReplacementConfig>
) : ForwardingI18N<Component>(backing) {
    private fun replace(lines: List<Component>) = lines.map { line ->
        var current = line
        replacements.forEach { current = current.replaceText(it) }
        current
    }

    override fun make(key: String, args: I18NArgs<Component>): List<Component>? {
        return backing.make(key, args)?.let { replace(it) }
    }

    override fun safe(key: String, args: I18NArgs<Component>): List<Component> {
        return replace(backing.safe(key, args))
    }

    override fun withLocale(locale: Locale) = TextReplacerI18N(backing.withLocale(locale), replacements)
}
