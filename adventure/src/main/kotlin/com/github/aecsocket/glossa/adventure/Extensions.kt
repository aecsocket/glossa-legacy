package com.github.aecsocket.glossa.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.JoinConfiguration

/** @see [append] */
operator fun Component.plus(like: ComponentLike) = append(like)
/** @see [append] */
operator fun Component.plus(component: Component) = append(component)
/** @see [append] */
operator fun Component.plus(builder: ComponentBuilder<*, *>) = append(builder)

/** @see [append] */
operator fun <B : ComponentBuilder<*, B>> B.plus(like: ComponentLike): B = append(like)
/** @see [append] */
operator fun <B : ComponentBuilder<*, B>> B.plus(component: Component): B = append(component)
/** @see [append] */
operator fun <B : ComponentBuilder<*, B>> B.plus(builder: ComponentBuilder<*, *>): B = append(builder)

/** @see [join] */
fun Iterable<ComponentLike>.join(config: JoinConfiguration = JoinConfiguration.newlines()) = Component.join(config, this)
