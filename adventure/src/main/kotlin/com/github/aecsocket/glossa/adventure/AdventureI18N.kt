package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.TemplatingI18N
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import java.util.Locale

/**
 * I18N service which localizes into Adventure [Component]s.
 *
 * For [safe] calls, returns a list with the key passed as a [net.kyori.adventure.text.TextComponent].
 */
abstract class AdventureI18N(
    locale: Locale = Locale.ROOT
) : TemplatingI18N<Component>(locale) {
    override fun safe(locale: Locale, key: String, args: ArgumentMap<Component>) =
        get(locale, key, args) ?: listOf(text(key))
}
