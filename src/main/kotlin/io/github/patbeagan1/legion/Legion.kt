package io.github.patbeagan1.legion

import io.github.patbeagan1.legion.visualization.LinkCollection
import io.github.patbeagan1.legion.visualization.Visualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector

/**
 * Represents an asynchronous typesafe task graph that processes elements of type [T].
 *
 * Legion provides a framework for defining, connecting, and executing a network of processing nodes.
 * Each node (imp) in the graph can transform, filter, or route the data flowing through the system.
 *
 * @param T The type of data being processed by this Legion instance
 * @property scope The scope that provides context and communication channels for this Legion
 * @property start The entry point node for the task graph
 */
class Legion<T>(
    val scope: LegionScope<T>,
    private val start: ImpNode<T, T>
) {
    /**
     * Accepts an item to be processed through the task graph.
     *
     * This method initiates the processing of an item by passing it to the start node
     * of the graph. The item will flow through the connected nodes according to the
     * defined graph structure.
     *
     * @param item The item to be processed
     */
    suspend fun accept(item: T) {
        start.accept(scope, scope.coroutineScope, item)
    }

    /**
     * Collects lifecycle events emitted during the execution of the task graph.
     *
     * This method allows monitoring the execution flow of tasks through the graph
     * by collecting events such as when nodes start processing, complete, or transmit data.
     *
     * @param collector The collector that will receive lifecycle events
     */
    suspend fun listen(collector: FlowCollector<LifecycleEvent>) {
        scope.eventStream.collect(collector)
    }

    /**
     * Sends a typed message to the Legion's scope using the specified key.
     *
     * This method enables communication between different parts of the task graph
     * using a message passing system.
     *
     * @param T The type of message being sent, must be a subtype of LegionScope.Message
     * @param messageKey The key used to identify this message type
     * @param message The message object to send
     */
    suspend inline fun <reified T : LegionScope.Message> sendMessage(messageKey: String, message: T) {
        scope.sendMessage(messageKey, message)
    }

    /**
     * Collects all links in the task graph starting from the entry point.
     *
     * @param linkCollection The collection to which links will be added
     * @return The updated link collection containing all graph connections
     */
    private fun collectLinks(linkCollection: LinkCollection): LinkCollection {
        start.collectLinks(subgraphStack = LinkCollection.SubgraphStack(), linkCollection)
        return linkCollection
    }

    /**
     * Generates a Graphviz representation of the task graph.
     *
     * This method creates a visual representation of the graph structure that can
     * be rendered using Graphviz tools.
     *
     * @return A string containing the Graphviz DOT representation of the graph
     */
    fun asGraphviz(): String = LinkCollection()
        .let { collectLinks(it) }
        .let { Visualizer.Graphviz(it) }
        .resolve()
}

/**
 * Creates and configures a new Legion instance with the specified parameters.
 *
 * This builder function simplifies the creation of Legion instances by providing
 * sensible defaults and a DSL-style configuration block.
 *
 * @param T The type of data to be processed by the Legion
 * @param start The entry point node, defaults to a simple passthrough node
 * @param debugLevel Controls the verbosity of logging, defaults to ERROR level
 * @param scope Optional coroutine scope, defaults to a new scope with Dispatchers.Default
 * @param action A lambda with receiver that configures the Legion's scope
 * @return A configured Legion<T> instance ready to process items
 */
fun <T> legion(
    start: ImpNode<T, T> = ImpNode.Imp("start") { it },
    debugLevel: LegionScope.DebugLevel = LegionScope.DebugLevel.ERROR,
    scope: CoroutineScope? = null,
    action: LegionScope<T>.() -> Unit
): Legion<T> = Legion(
    LegionScope(
        start,
        debugLevel,
        scope ?: CoroutineScope(Dispatchers.Default)
    ).apply(action),
    start
)