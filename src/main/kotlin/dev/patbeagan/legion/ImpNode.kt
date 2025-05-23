package dev.patbeagan.legion

import dev.patbeagan.legion.subgraphs.Cohort
import dev.patbeagan.legion.visualization.LinkCollection
import kotlinx.coroutines.*
import java.util.UUID


interface ImpNode<EventIn, EventOut> : Acceptor<EventIn> {
    fun <EventOther> LegionScope<*>.link(c: ImpNode<EventOut, EventOther>): ImpNode<EventOut, EventOther>

    val uuid: UUID
    val name: String
    var isSubgraphEnd: Boolean

    context(LegionScope<*>)
    operator fun <EventOther> rangeTo(
        other: ImpNode<in EventOut, EventOther>
    ): ImpNode<EventOut, EventOther> = link(other as ImpNode<EventOut, EventOther>)

    context(LegionScope<*>)
    operator fun rangeTo(
        other: List<ImpNode<in EventOut, *>>
    ) = other.forEach { link(it as ImpNode<EventOut, *>) }

    context(LegionScope<*>)
    operator fun <EventOther> rangeTo(
        other: suspend (EventOut) -> EventOther
    ): ImpNode<EventOut, EventOther> = link(imp(null, other))

    context(Cohort.CohortScope<I, O>, LegionScope<*>)
    operator fun <I, O, EventOther> rangeTo(
        other: suspend (EventOut) -> EventOther
    ): ImpNode<EventOut, EventOther> = link(impCohort(other))

    fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection)

    abstract class BaseImp<EIn, EOut> : ImpNode<EIn, EOut> {
        abstract suspend fun action(input: EIn): EOut
        private val imp by lazy { Imp(name, ::action) }
        override val uuid: UUID = UUID.randomUUID()
        override var isSubgraphEnd: Boolean = false

        override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
            TODO("This needs to be overridden before the graph can be visualized.")
        }

        override suspend fun accept(legionScope: LegionScope<*>, coroutineScope: CoroutineScope, e: EIn) =
            imp.accept(legionScope, coroutineScope, e)

        override fun <EventOther> LegionScope<*>.link(c: ImpNode<EOut, EventOther>): ImpNode<EOut, EventOther> =
            imp.rangeTo(c)
    }

    class Imp<EIn, EOut>(
        nameIn: String,
        val action: suspend (EIn) -> EOut?
    ) : ImpNode<EIn, EOut> {
        override val uuid: UUID = UUID.randomUUID()
        override val name: String = "$nameIn-($uuid)"
        private val links = mutableListOf<ImpNode<EOut, *>>()
        override var isSubgraphEnd: Boolean = false

        override suspend fun accept(legionScope: LegionScope<*>, coroutineScope: CoroutineScope, e: EIn) {
            legionScope.log("acc: $e to: $name -> ${links.map { it.name }}")
            val threadName = Thread.currentThread().name
            legionScope.eventStream.emit(LifecycleEvent.Running(name, threadName))
            action(e).let {
                links.forEach { link ->
                    legionScope.eventStream.emit(
                        LifecycleEvent.Transmitting(
                            name,
                            link.name,
                            threadName
                        )
                    )
                    if (it != null) {
                        coroutineScope.launch {
                            link.accept(legionScope, this@launch, it)
                        }
                    }
                }
            }
            legionScope.eventStream.emit(LifecycleEvent.Waiting(name, threadName))
        }

        override fun <EventOther> LegionScope<*>.link(c: ImpNode<EOut, EventOther>): ImpNode<EOut, EventOther> {
            links.add(c)
            return c
        }

        override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
            linkCollection.recordLinkHard(this to links)
            linkCollection.subgraph(subgraphStack, this)
            if (isSubgraphEnd) {
                subgraphStack.pop()
            }
            links.forEach { it.collectLinks(subgraphStack, linkCollection) }
        }
    }
}
