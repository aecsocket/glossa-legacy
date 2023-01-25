package io.gitlab.aecsocket.glossa.core

import java.util.*

/**
 * An I18N which delegates operations to a backing service.
 * @param backing The backing service to which operations are delegated.
 */
abstract class ForwardingI18N<T>(val backing: I18N<T>) : I18N<T> {
    override fun make(key: String, args: I18NArgs<T>) = backing.make(key, args)
    override fun safe(key: String, args: I18NArgs<T>) = backing.safe(key, args)
    override fun Iterable<T>.join(separator: T) = backing.run { join(separator) }
    override val empty get() = backing.empty
    override val newline get() = backing.newline
}
