package io.github.patbeagan1.legion

import kotlinx.coroutines.flow.MutableSharedFlow

fun <EIn, EOut> impGlobal(action: (EIn) -> EOut) = ImpNode.Imp("unknown", action)

fun <T> LegionScope<*>.sink(flow: MutableSharedFlow<T>): ImpNode.Imp<T, Nothing> = imp {
    flow.emit(it)
    null
}