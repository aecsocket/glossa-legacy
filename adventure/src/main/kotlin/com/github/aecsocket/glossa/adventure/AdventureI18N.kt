package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.Args
import com.github.aecsocket.glossa.core.TemplatingI18N
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import java.util.Locale

abstract class AdventureI18N(
    locale: Locale = Locale.ROOT
) : TemplatingI18N<Component>(locale) {
    override fun safe(locale: Locale, key: String, args: Args) =
        get(locale, key, args) ?: listOf(text(key))
}
