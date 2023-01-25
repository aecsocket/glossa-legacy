package io.gitlab.aecsocket.glossa.core

/**
 * An object which can be expressed by a message created by an [I18N] instance.
 */
interface Localizable<T> {
    /**
     * Creates the message representing this object.
     *
     * The locale of this I18N object will already be configured correctly.
     */
    fun localize(i18n: I18N<T>): T
}
