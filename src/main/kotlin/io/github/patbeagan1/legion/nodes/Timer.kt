package io.github.patbeagan1.legion.nodes

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration

fun <T> LegionScope<*>.timer(
    duration: Duration
): ImpNode<T, T> = imp("timer-${Random.Default.nextInt()}") {
    delay(duration.inWholeMilliseconds); it
}

