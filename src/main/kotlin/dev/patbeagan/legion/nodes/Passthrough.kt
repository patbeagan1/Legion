package dev.patbeagan.legion.nodes

import dev.patbeagan.legion.ImpNode
import dev.patbeagan.legion.LegionScope

fun <T> LegionScope<*>.passthrough(name: String): ImpNode<T, T> = imp(name) { it }
fun <T> passthrough(name: String): ImpNode<T, T> = ImpNode.Imp(name) { it }