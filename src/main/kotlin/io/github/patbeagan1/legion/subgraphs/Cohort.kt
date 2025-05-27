package io.github.patbeagan1.legion.subgraphs

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope
import io.github.patbeagan1.legion.visualization.LinkCollection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.util.UUID

class Cohort<I, O>(
    nameIn: String,
    private val start: ImpNode<I, I>,
    private val end: ImpNode<O, O>,
    private val errorCatch: ImpNode<Throwable, Throwable>,
    private val dispatcher: CoroutineDispatcher
) : ImpNode<I, O> {
    override val uuid: UUID = UUID.randomUUID()
    override val name: String = "cohort-$nameIn"
    override var isSubgraphEnd: Boolean = false

    override suspend fun accept(legionScope: LegionScope<*>, coroutineScope: CoroutineScope, e: I) {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("CoroutineExceptionHandler")
            coroutineScope.launch(dispatcher) { errorCatch.accept(legionScope, coroutineScope, exception) }
        }
//        supervisorScope {
//        coroutineScope {

        (coroutineScope + handler).launch(dispatcher) {
            try {
                start.accept(legionScope, coroutineScope + handler, e)
            } catch (t: Throwable) {
                println(":caught $t")
                errorCatch.accept(legionScope, coroutineScope, t)
            }
        }
//        }
//        }
    }

    override fun <EventOther> LegionScope<*>.link(c: ImpNode<O, EventOther>): ImpNode<O, EventOther> = end..c
    override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
        end.isSubgraphEnd = true
        linkCollection.recordLinkHard(this to listOf(start))
        start.collectLinks(subgraphStack = subgraphStack.push(LinkCollection.SubgraphType.Cohort(this.name)), linkCollection)
    }

    @DslMarker
    annotation class CohortScopeMarker

    @CohortScopeMarker
    class CohortScope<I, O>(
        val startCohort: ImpNode<I, I>,
        val endCohort: ImpNode<O, O>,
        val errorCatch: ImpNode<Throwable, Throwable>
    ) {
        private var counter = 0

        fun <EIn, EOut> impCohort(action: suspend (EIn) -> EOut) = ImpNode.Imp("cohort-imp-${counter++}", action)
    }
}