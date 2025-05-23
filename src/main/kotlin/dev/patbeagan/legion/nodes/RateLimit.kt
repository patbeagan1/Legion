package dev.patbeagan.legion.nodes

import dev.patbeagan.legion.ImpNode
import dev.patbeagan.legion.LegionScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration

fun <I, O> LegionScope<*>.rateLimit(timeWindow: Duration, action: suspend (I) -> O): ImpNode<I, O> {
    val mutex = Mutex() // Only one permit
    var lastRequestTime = 0L // Timestamp of the last request
    return imp {
        withContext(Dispatchers.Unconfined) {
            mutex.withLock {
                val elapsed = System.currentTimeMillis() - lastRequestTime
                if (elapsed < timeWindow.inWholeMilliseconds) {
                    val timeMillis = timeWindow.inWholeMilliseconds - elapsed
                    // Ensure timeWindow between requests
                    log("Waiting for $timeMillis")
                    delay(timeMillis)
                }
                lastRequestTime = System.currentTimeMillis()
                action(it)
            }
        }
    }
}