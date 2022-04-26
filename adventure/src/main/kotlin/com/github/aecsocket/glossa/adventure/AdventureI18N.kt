package com.github.aecsocket.glossa.adventure

import com.github.aecsocket.glossa.core.AbstractI18N
import com.github.aecsocket.glossa.core.I18NContext
import com.github.aecsocket.glossa.core.MultilineI18N
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import java.util.Locale

abstract class AdventureI18N(
    locale: Locale = Locale.ROOT
) : AbstractI18N<List<Component>>(locale), MultilineI18N<Component> {
    override fun safe(locale: Locale, key: String, args: Map<String, (I18NContext<List<Component>>) -> List<Component>>) =
        get(locale, key, args) ?: listOf(text(key))
}
