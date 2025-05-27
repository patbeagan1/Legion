package io.github.patbeagan1.legion.nodes

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope
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

