package com.github.aecsocket.glossa.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.JoinConfiguration

/** @see [Component.append] */
operator fun Component.plus(like: ComponentLike) = append(like)
/** @see [Component.append] */
operator fun Component.plus(component: Component) = append(component)
/** @see [Component.append] */
operator fun Component.plus(builder: ComponentBuilder<*, *>) = append(builder)

/** @see [Component.append] */
operator fun <B : ComponentBuilder<*, B>> B.plus(like: ComponentLike): B = append(like)
/** @see [Component.append] */
operator fun <B : ComponentBuilder<*, B>> B.plus(component: Component): B = append(component)
/** @see [Component.append] */
operator fun <B : ComponentBuilder<*, B>> B.plus(builder: ComponentBuilder<*, *>): B = append(builder)

/** @see [Component.join] */
fun Iterable<ComponentLike>.join(config: JoinConfiguration = JoinConfiguration.newlines()) = Component.join(config, this)
