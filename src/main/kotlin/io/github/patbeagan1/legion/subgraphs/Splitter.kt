package io.github.patbeagan1.legion.subgraphs

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope
import io.github.patbeagan1.legion.visualization.LinkCollection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.util.UUID

class Splitter<IT, I : Iterable<IT>, OT>(
    private val start: ImpNode<IT, IT>,
    private val end: ImpNode<OT, OT>,
    private val dispatcher: CoroutineDispatcher,
) : ImpNode<I, List<OT>> {
    override val uuid: UUID = UUID.randomUUID()
    override val name: String = "splitter-collection-start-$uuid"
    override var isSubgraphEnd: Boolean = false

    private var size: Int = 0
    val state = mutableListOf<OT>()
    val endCollection = ImpNode.Imp<OT, List<OT>>("splitter-collection-end") {
        state.add(it)
        if (state.size == size) state.toList() else null
    }

    override suspend fun accept(legionScope: LegionScope<*>, coroutineScope: CoroutineScope, e: I) {
        withContext(dispatcher) {
            size = e.count()
            e.forEach {
                start.accept(legionScope, coroutineScope, it)
            }
        }
    }

    override fun <EventOther> LegionScope<*>.link(c: ImpNode<List<OT>, EventOther>): ImpNode<List<OT>, EventOther> =
        end..endCollection..c

    override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
        linkCollection.recordLinkHard(this to listOf(start))
        end.isSubgraphEnd = true
        start.collectLinks(subgraphStack.push(LinkCollection.SubgraphType.Splitter(name)), linkCollection)
    }

    @DslMarker
    annotation class SplitterScopeMarker

    @SplitterScopeMarker
    class SplitterScope<I, O>(
        val startSplitter: ImpNode<I, I>,
        val endSplitter: ImpNode<O, O>
    )
}