package com.github.aecsocket.glossa.core

import java.util.Locale

sealed interface Argument<T> {
    interface State<T> {
        val backing: Argument<T>
    }

    fun createState(): State<T>

    interface Factory<T> {
        val name: String

        fun create(): Argument<T>

        fun interface Raw<T> : Factory<T> {
            override val name: String
                get() = "raw"
            override fun create(): Argument.Raw<T>
        }

        fun interface Substitution<T> : Factory<T> {
            override val name: String
                get() = "substitution"
            override fun create(): Argument.Substitution<T>
        }

        fun interface Scoped<T> : Factory<T> {
            override val name: String
                get() = "scoped"
            override fun create(): Argument.Scoped<T>
        }
    }

    data class Raw<T>(val value: Any): Argument<T> {
        inner class State : Argument.State<T> {
            override val backing: Raw<T>
                get() = this@Raw
        }

        override fun createState() = State()
    }

    data class Substitution<T>(val value: List<T>): Argument<T> {
        inner class State : Argument.State<T> {
            override val backing: Substitution<T>
                get() = this@Substitution
        }

        override fun createState() = State()
    }

    sealed interface Scoped<T> : Argument<T>

    data class ArgMap<T>(val args: Map<String, Factory<T>>): Scoped<T> {
        operator fun get(key: String) = args[key]

        inner class State : Argument.State<T> {
            override val backing: ArgMap<T>
                get() = this@ArgMap

            private val cache = HashMap<String, Argument.State<T>?>()

            fun backing(key: String) = args[key]

            // any factories we cache here, should be the same type when they come out
            @Suppress("UNCHECKED_CAST")
            fun <S : Argument.State<T>> compute(key: String, value: Factory<T>): S =
                cache.computeIfAbsent(key) { value.create().createState() } as S

            fun compute(key: String) = cache.computeIfAbsent(key) { args[key]?.create()?.createState() }
        }

        override fun createState() = State()
    }

    data class ArgList<T>(val args: List<Argument<T>>): Scoped<T> {
        inner class State : Argument.State<T> {
            override val backing: ArgList<T>
                get() = this@ArgList

            private val cache: ArrayList<Argument.State<T>>? = null

            fun compute(): List<Argument.State<T>> = cache ?: this@ArgList.args.map { it.createState() }
        }

        override fun createState() = State()
    }

    companion object {
        fun <T> buildMap(i18n: I18N<T>, value: MapScope<T>.() -> Unit): ArgMap<T> {
            val args = HashMap<String, Factory<T>>()
            value(MapScope(i18n) { k, v -> args[k.validate()] = v })
            return ArgMap(args)
        }

        fun <T> buildList(i18n: I18N<T>, value: ListScope<T>.() -> Unit): ArgList<T> {
            val args = ArrayList<Argument<T>>()
            value(ListScope(i18n, args::add))
            return ArgList(args)
        }

        fun <T> single(value: Factory<T>) = ArgMap(mapOf(THIS_ARG to value))
    }

    sealed class Scope<T>(
        i18n: I18N<T>
    ) : ForwardingI18N<T>(i18n) {
        fun rawArg(value: Any) = Raw<T>(value)

        fun subArg(value: List<T>) = Substitution(value)

        fun tlArg(value: Localizable<T>) = Substitution(value.localize(withLocale(locale)))

        fun mapArg(value: MapScope<T>.() -> Unit) = buildMap(this, value)

        fun listArg(value: ListScope<T>.() -> Unit) = buildList(this, value)
    }

    // all lazy loaded
    class MapScope<T>(
        i18n: I18N<T>,
        val add: (String, Factory<T>) -> Unit
    ) : Scope<T>(i18n) {
        fun arg(key: String, arg: Factory<T>) = add(key, arg)

        fun raw(key: String, value: () -> Any) = arg(key, Factory.Raw { rawArg(value()) })

        fun sub(key: String, value: () -> List<T>) = arg(key, Factory.Substitution { subArg(value()) })

        fun tl(key: String, value: () -> Localizable<T>) = arg(key, Factory.Substitution { tlArg(value()) })

        // use a SAM constructor here because we need to provide a type for Factory's A
        fun map(key: String, value: MapScope<T>.() -> Unit) = arg(key, Factory.Scoped { mapArg(value) })

        fun list(key: String, value: ListScope<T>.() -> Unit) = arg(key, Factory.Scoped { listArg(value) })
    }

    // all non-lazy loaded
    class ListScope<T>(
        i18n: I18N<T>,
        val add: (Argument<T>) -> Unit
    ) : Scope<T>(i18n) {
        fun arg(arg: Argument<T>) = add(arg)

        fun raw(value: Any) = arg(rawArg(value))

        fun sub(value: List<T>) = arg(subArg(value))

        fun tl(value: Localizable<T>) = arg(tlArg(value))

        fun map(value: MapScope<T>.() -> Unit) = arg(mapArg(value))

        fun list(value: ListScope<T>.() -> Unit) = arg(listArg(value))
    }
}
