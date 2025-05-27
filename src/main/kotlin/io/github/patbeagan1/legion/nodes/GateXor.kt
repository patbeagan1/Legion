package io.github.patbeagan1.legion.nodes

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope
import io.github.patbeagan1.legion.visualization.LinkCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class GateXor<EIn>(
    private val actions: List<Pair<(EIn) -> Boolean, ImpNode<EIn, *>>>,
) : ImpNode<EIn, Nothing> {
    override val uuid: UUID = UUID.randomUUID()
    override val name: String = "gateXor-${uuid}"
    override var isSubgraphEnd: Boolean = false

    override suspend fun accept(scope: LegionScope<*>, coroutineScope: CoroutineScope, e: EIn) {
        actions.firstOrNull { it.first(e) }?.second?.let {
            coroutineScope.launch {
                it.accept(scope, this, e)
            }
        }
    }

    override fun <EventOther> LegionScope<*>.link(c: ImpNode<Nothing, EventOther>): ImpNode<Nothing, EventOther> {
        throw IllegalAccessError(buildString {
            append("GateXor cannot be normally linked, ")
            append("because it needs to dynamically decide which links are valid ")
            append("given a list of conditions.")
        })
    }

    override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
        linkCollection.recordLinkSoft(this to actions.map { it.second })
    }
}

fun <EIn> LegionScope<*>.gateXor(
    links: List<Pair<(it: EIn) -> Boolean, ImpNode<EIn, *>>>
) = GateXor(
    links
)