@file:JvmName("ImplPartialQuantified2")

package io.github.patbeagan1.legion.joins

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope
import io.github.patbeagan1.legion.LifecycleEvent
import java.util.UUID
import io.github.patbeagan1.legion.visualization.LinkCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Data class to hold two typed parameters for partial quantified node processing.
 *
 * @param T1 First parameter type
 * @param T2 Second parameter type
 * @property first List of values of type T1
 * @property second List of values of type T2
 */
data class ImplPartialQuantified2Params<T1, T2>(
    val first: List<T1>,
    val second: List<T2>
)

/**
 * Creates a node that processes input events of specific types with quantified constraints.
 * This version handles two different event types.
 *
 * @param EIn Common supertype of all input event types
 * @param T1 First specific event type
 * @param T2 Second specific event type
 * @param EOut Output event type
 * @param name Name of the node
 * @param quantity1 Required quantity of T1 events
 * @param quantity2 Required quantity of T2 events
 * @param action Function that processes the collected events when quantity requirements are met
 * @return A node that collects events until quantity requirements are met, then processes them
 */
inline fun <EIn : Any,
        reified T1 : EIn,
        reified T2 : EIn,
        reified EOut : Any
        > impJoinAllQuantified(
    name: String,
    quantity1: Quantity<T1>,
    quantity2: Quantity<T2>,
    noinline action: suspend (ImplPartialQuantified2Params<T1, T2>) -> EOut
): ImpNode<EIn, EOut> = object : ImpNode<EIn, EOut> {
    override val name: String = name
    override var isSubgraphEnd: Boolean = false
    override val uuid: UUID = UUID.randomUUID()
    
    private val firstSet = mutableListOf<T1>()
    private val secondSet = mutableListOf<T2>()
    
    private val links = mutableListOf<ImpNode<EOut, *>>()

    override suspend fun accept(legionScope: LegionScope<*>, coroutineScope: CoroutineScope, e: EIn) {
        val threadName = Thread.currentThread().name
        legionScope.eventStream.emit(LifecycleEvent.Running(name, threadName))
        
        when (e) {
            is T1 -> firstSet.add(e)
            is T2 -> secondSet.add(e)
        }
        
        if (firstSet.size >= quantity1.value && 
            secondSet.size >= quantity2.value) {
            
            val params = ImplPartialQuantified2Params(
                firstSet.take(quantity1.value),
                secondSet.take(quantity2.value)
            )
            
            // Remove used items
            repeat(quantity1.value) { firstSet.removeAt(0) }
            repeat(quantity2.value) { secondSet.removeAt(0) }
            
            val result = action(params)
            
            links.forEach { link ->
                legionScope.eventStream.emit(
                    LifecycleEvent.Transmitting(
                        name,
                        link.name,
                        threadName
                    )
                )
                coroutineScope.launch {
                    link.accept(legionScope, this, result)
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
