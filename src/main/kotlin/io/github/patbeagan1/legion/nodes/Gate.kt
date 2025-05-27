package io.github.patbeagan1.legion.nodes

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope

fun <T> LegionScope<*>.gate(condition: (T) -> Boolean): ImpNode.Imp<T, T> = imp("gate") {
    it.takeIf(condition)
}