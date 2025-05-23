package dev.patbeagan.legion.nodes

import dev.patbeagan.legion.ImpNode
import dev.patbeagan.legion.LegionScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <EIn, EOut> LegionScope<*>.memo(action: suspend (EIn) -> EOut): ImpNode.Imp<EIn, EOut> {
    val memoTable: MutableMap<EIn, EOut> = mutableMapOf()
    val mutex = Mutex()
    return imp {
        log("memoTable: $memoTable")
        return@imp mutex.withLock {
            memoTable.computeIfAbsent(it) {
                runBlocking { action(it) }
            }
        }
    }
}

