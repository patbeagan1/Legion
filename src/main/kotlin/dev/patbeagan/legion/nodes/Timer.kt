package dev.patbeagan.legion.nodes

import dev.patbeagan.legion.ImpNode
import dev.patbeagan.legion.LegionScope
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration

fun <T> LegionScope<*>.timer(
    duration: Duration
): ImpNode<T, T> = imp("timer-${Random.Default.nextInt()}") {
    delay(duration.inWholeMilliseconds); it
}

