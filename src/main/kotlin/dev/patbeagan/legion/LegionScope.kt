package dev.patbeagan.legion

import dev.patbeagan.legion.nodes.passthrough
import dev.patbeagan.legion.subgraphs.Cohort
import dev.patbeagan.legion.subgraphs.Cohort.CohortScope
import dev.patbeagan.legion.subgraphs.Splitter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow

@DslMarker
annotation class LegionScopeMarker

@LegionScopeMarker
class LegionScope<I>(
    val start: ImpNode<I, I> = passthrough("start"),
    private val debugLevel: DebugLevel,
    val coroutineScope: CoroutineScope,
) {
    private var counter = 0
    val eventStream = MutableSharedFlow<LifecycleEvent>()
    val messageHandler = MessageHandler(::log)

    fun <EIn, EOut> imp(name: String? = null, action: suspend (EIn) -> EOut?) =
        ImpNode.Imp(name ?: "imp-${counter++}", action)

    fun log(message: String, level: DebugLevel = DebugLevel.INFO) {
        if (level.ordinal >= debugLevel.ordinal) {
            println("$level: $message")
        }
    }

    suspend inline fun <reified T : Message> awaitMessage(messageKey: String): T {
        eventStream.emit(LifecycleEvent.Ready(messageKey, Thread.currentThread().name))
        val m = messageHandler.awaitMessage<T>(messageKey)
        eventStream.emit(LifecycleEvent.Running(messageKey, Thread.currentThread().name))
        return m
    }

    suspend inline fun <reified T : Message> awaitMessageQueued(messageKey: String): T {
        eventStream.emit(LifecycleEvent.Ready(messageKey, Thread.currentThread().name))
        val m = messageHandler.awaitMessageQueued<T>(messageKey)
        eventStream.emit(LifecycleEvent.Running(messageKey, Thread.currentThread().name))
        return m
    }

    fun <I, O> cohort(
        name: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        start: ImpNode<I, I> = ImpNode.Imp("startCohort") { it },
        end: ImpNode<O, O> = ImpNode.Imp("endCohort") { it },
        errorCatch: ImpNode<Throwable, Throwable> = ImpNode.Imp("errorCatch") { it },
        action: Cohort.CohortScope<I, O>.() -> Unit
    ): ImpNode<I, O> = Cohort(
        name,
        start,
        end,
        errorCatch,
        dispatcher,
    ).also { CohortScope(start, end, errorCatch).apply(action) }

    fun <IT, I : Collection<IT>, OT> splitter(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        start: ImpNode<IT, IT> = ImpNode.Imp("startSplitter") { it },
        end: ImpNode<OT, OT> = ImpNode.Imp("endSplitter") { it },
        action: Splitter.SplitterScope<IT, OT>.() -> Unit
    ): ImpNode<I, List<OT>> = Splitter<IT, I, OT>(
        start,
        end,
        dispatcher,
    ).also { Splitter.SplitterScope(start, end).apply(action) }

    suspend fun <T : Message> sendMessage(messageKey: String, message: T) {
        messageHandler.sendMessage(messageKey, message)
    }

    suspend fun <T : Message> sendMessageQueued(messageKey: String, message: T) {
        messageHandler.sendMessageQueued(messageKey, message)
    }

    interface Message
    interface Output

    enum class DebugLevel {
        DEBUG, INFO, WARN, ERROR
    }
}
