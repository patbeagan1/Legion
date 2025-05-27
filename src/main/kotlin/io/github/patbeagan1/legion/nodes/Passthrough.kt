package io.github.patbeagan1.legion.nodes

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope

fun <T> LegionScope<*>.passthrough(name: String): ImpNode<T, T> = imp(name) { it }
fun <T> passthrough(name: String): ImpNode<T, T> = ImpNode.Imp(name) { it }