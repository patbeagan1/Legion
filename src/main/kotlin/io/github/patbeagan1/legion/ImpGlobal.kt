package io.github.patbeagan1.legion

fun <EIn, EOut> impGlobal(action: (EIn) -> EOut) = ImpNode.Imp("unknown", action)