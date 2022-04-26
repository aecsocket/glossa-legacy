package com.github.aecsocket.glossa.adventure

import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.ComponentLike

operator fun Component.plus(like: ComponentLike) = append(like)
operator fun Component.plus(component: Component) = append(component)
operator fun Component.plus(builder: ComponentBuilder<*, *>) = append(builder)

operator fun <B : ComponentBuilder<*, B>> B.plus(like: ComponentLike): B = append(like)
operator fun <B : ComponentBuilder<*, B>> B.plus(component: Component): B = append(component)
operator fun <B : ComponentBuilder<*, B>> B.plus(builder: ComponentBuilder<*, *>): B = append(builder)
