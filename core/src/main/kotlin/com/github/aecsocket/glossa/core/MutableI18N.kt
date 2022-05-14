package com.github.aecsocket.glossa.core

import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.loader.ConfigurationLoader
import java.util.*

/**
 * A service to localize a given key based on a locale,
 * generating a [T].
 *
 * Arguments are provided as [A] instances.
 *
 * Translations can be registered and unregistered.
 */
interface MutableI18N<T, A> : I18N<T, A> {
    /**
     * Registers a translation.
     * @param tl Translation.
     */
    fun register(tl: Translation)

    /**
     * Registers a translation.
     * @param locale Translation locale.
     * @param tl Translation pairs.
     */
    fun register(locale: Locale, vararg tl: Pair<String, String>) =
        register(Translation(locale, tl.associate { it }))

    /**
     * Unregisters all translations.
     */
    fun clear()
}

private val CONFIG_OPTIONS = ConfigurationOptions.defaults()
    .serializers {
        it.register(Translation::class, Translation.Serializer)
    }

/**
 * Loads translations from a Configurate loader into this I18N.
 *
 * **HOCON example**
 * ```hocon
 * __locale__: "en-US" # (required) Locale.forLanguageTag
 * # Note that, if using `.` in keys, the quotes are required
 * "message.one": "Message one"
 * "message.multiline": [
 *   "Line one"
 *   "Line two"
 * ]
 * ```
 * @param loader Loader.
 */
@Throws(ConfigurateException::class)
fun MutableI18N<*, *>.loadTranslations(loader: ConfigurationLoader<*>) {
    register(loader.load(CONFIG_OPTIONS).req(Translation::class))
}
