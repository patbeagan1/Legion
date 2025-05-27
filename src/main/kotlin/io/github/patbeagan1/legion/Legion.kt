package io.github.patbeagan1.legion

import io.github.patbeagan1.legion.visualization.LinkCollection
import io.github.patbeagan1.legion.visualization.Visualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector

class Legion<T>(
    val scope: LegionScope<T>,
    private val start: ImpNode<T, T>
) {
    suspend fun accept(item: T) {
        start.accept(scope, scope.coroutineScope, item)
    }

    suspend fun listen(collector: FlowCollector<LifecycleEvent>) {
        scope.eventStream.collect(collector)
    }

    suspend inline fun <reified T : LegionScope.Message> sendMessage(messageKey: String, message: T) {
        scope.sendMessage(messageKey, message)
    }

    private fun collectLinks(linkCollection: LinkCollection): LinkCollection {
        start.collectLinks(subgraphStack = LinkCollection.SubgraphStack(), linkCollection)
        return linkCollection
    }

    fun asGraphviz(): String = LinkCollection()
        .let { collectLinks(it) }
        .let { Visualizer.Graphviz(it) }
        .resolve()
}

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