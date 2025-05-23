package dev.patbeagan.legion.nodes

import dev.patbeagan.legion.ImpNode
import dev.patbeagan.legion.LegionScope

fun <T> LegionScope<*>.gate(condition: (T) -> Boolean): ImpNode.Imp<T, T> = imp("gate") {
    it.takeIf(condition)
}