package com.github.aecsocket.glossa.api

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
     * @param tl the translation.
     */
    fun register(tl: Translation)

    /**
     * Registers a translation.
     * @param locale the translation locale.
     * @param tl the translation pairs.
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

@Throws(ConfigurateException::class)
fun MutableI18N<*, *>.load(loader: ConfigurationLoader<*>) {
    register(loader.load(CONFIG_OPTIONS).req(Translation::class))
}
