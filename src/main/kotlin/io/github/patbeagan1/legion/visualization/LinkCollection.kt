package io.github.patbeagan1.legion.visualization

import io.github.patbeagan1.legion.ImpNode

private typealias MetaChan = ImpNode<out Any?, out Any?>

class LinkCollection {
    private val _linksHard = mutableMapOf<MetaChan, MutableSet<MetaChan>>()
    val linksHard = _linksHard as Map<MetaChan, Set<MetaChan>>

    private val _linksSoft = mutableMapOf<MetaChan, MutableSet<MetaChan>>()
    val linksSoft = _linksSoft as Map<MetaChan, Set<MetaChan>>

    val subgraphs = mutableMapOf<String, MutableList<MetaChan>>()

    fun subgraph(subgraphStack: SubgraphStack, metaChan: MetaChan) {
        when (val subgraphType = subgraphStack.peek()) {
            is SubgraphType.Cohort -> subgraphs
                .computeIfAbsent(subgraphType.parentName) { mutableListOf() }
                .add(metaChan)

            is SubgraphType.Splitter -> subgraphs
                .computeIfAbsent(subgraphType.parentName) { mutableListOf() }
                .add(metaChan)

            SubgraphType.None,
            null -> Unit
        }
    }

    fun recordLinkHard(link: Pair<MetaChan, List<MetaChan>>) {
        _linksHard.computeIfAbsent(link.first) { mutableSetOf() }.addAll(link.second)
    }

    fun recordLinkSoft(link: Pair<MetaChan, List<MetaChan>>) {
        _linksSoft.computeIfAbsent(link.first) { mutableSetOf() }.addAll(link.second)
    }

    sealed class SubgraphType {
        data class Cohort(val parentName: String) : SubgraphType()
        data class Splitter(val parentName: String) : SubgraphType()
        data object None : SubgraphType()
    }

    @JvmInline
    value class SubgraphStack(private val stack: MutableList<SubgraphType> = mutableListOf()) {

        fun peek(): SubgraphType? = stack.lastOrNull()

        fun push(subgraphType: SubgraphType): SubgraphStack {
            stack.add(subgraphType)
            return this
        }

        fun pop(): SubgraphStack {
            stack.removeLastOrNull()
            return this
        }
    }
}