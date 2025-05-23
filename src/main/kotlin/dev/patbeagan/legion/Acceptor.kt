package dev.patbeagan.legion

import kotlinx.coroutines.CoroutineScope

interface Acceptor<EventIn> {
    suspend fun accept(legionScope: LegionScope<*>, coroutineScope: CoroutineScope, e: EventIn)
}