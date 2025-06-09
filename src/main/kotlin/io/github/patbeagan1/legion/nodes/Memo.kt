package io.github.patbeagan1.legion.nodes

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Creates a memoized version of the provided suspending function.
 * The function caches results for previously seen inputs to avoid re-computation.
 * Thread-safe implementation using a mutex to protect the memoization table.
 *
 * @param EIn The input type for the function to be memoized
 * @param EOut The output type for the function to be memoized
 * @param action The suspending function to be memoized
 * @return An [ImpNode.Imp] that performs the memoized computation
 *
 * Example usage:
 * ```
 * val fibonacci = memo<Int, Int> { n ->
 *     when (n) {
 *         0, 1 -> n
 *         else -> fibonacci(n - 1) + fibonacci(n - 2)
 *     }
 * }
 * ```
 */
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

