package io.gitlab.aecsocket.glossa.adventure

import io.gitlab.aecsocket.glossa.core.I18N
import io.gitlab.aecsocket.glossa.core.I18NArgs
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.*
import net.kyori.adventure.text.JoinConfiguration

interface AdventureI18N : I18N<Component> {
    override fun safe(key: String, args: I18NArgs<Component>): List<Component> {
        return make(key, args) ?: listOf(text(key))
    }

    override val empty get() = empty()
    override val newline get() = newline()

    override fun Iterable<Component>.join(separator: Component) =
        join(JoinConfiguration.separator(separator), this)
}
