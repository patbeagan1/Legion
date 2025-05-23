package dev.patbeagan.legion

fun <EIn, EOut> impGlobal(action: (EIn) -> EOut) = ImpNode.Imp("unknown", action)