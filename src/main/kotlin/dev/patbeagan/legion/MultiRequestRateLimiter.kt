package dev.patbeagan.legion

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration

class MultiRequestRateLimiter(maxConcurrent: Int, private val timeWindow: Duration) {
    private val semaphore = Semaphore(maxConcurrent) // Only one permit
    private var lastRequestTime = 0L // Timestamp of the last request

    suspend fun <T> limit(action: suspend () -> T): T {
        semaphore.withPermit {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime

            // Ensure timeWindow between requests
            if (elapsed < timeWindow.inWholeMilliseconds) {
                delay(timeWindow.inWholeMilliseconds - elapsed)
            }

            lastRequestTime = System.currentTimeMillis()
            return action()
        }
    }
}