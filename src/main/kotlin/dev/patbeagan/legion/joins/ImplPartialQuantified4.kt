package dev.patbeagan.legion.joins

import dev.patbeagan.legion.ImpNode
import dev.patbeagan.legion.LegionScope
import dev.patbeagan.legion.visualization.LinkCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

@JvmInline
value class Quantity<T>(val value: Int)

inline fun <EIn : Any,
        reified T1 : EIn,
        reified T2 : EIn,
        reified T3 : EIn,
        reified T4 : EIn,
        reified EOut : Any
        > impJoinAllQuantified(
    name: String,
    quantity1: Quantity<T1>,
    quantity2: Quantity<T2>,
    quantity3: Quantity<T3>,
    quantity4: Quantity<T4>,
    noinline action: suspend (ImplPartialQuantified4Params<T1, T2, T3, T4>) -> EOut
): ImpNode<EIn, EOut> = object : ImpNode<EIn, EOut> {
    override val name: String = name
    override var isSubgraphEnd: Boolean = false

    override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
        linkCollection.recordLinkHard(this to links)
        links.forEach { it.collectLinks(subgraphStack, linkCollection) }
    }

    private val links = mutableListOf<ImpNode<EOut, *>>()
    private var state = ImplPartialQuantified4Params.Builder<
            T1,
            T2,
            T3,
            T4,
            ImplPartialQuantified4Params<
                    T1,
                    T2,
                    T3,
                    T4>>(
        quantity1 = quantity1.value,
        quantity2 = quantity2.value,
        quantity3 = quantity3.value,
        quantity4 = quantity4.value,
    )

    override suspend fun accept(scope: LegionScope<*>, coroutineScope: CoroutineScope, e: EIn) {
        when {
            e as? T1 != null -> state.withParam1(e)
            e as? T2 != null -> state.withParam2(e)
            e as? T3 != null -> state.withParam3(e)
            e as? T4 != null -> state.withParam4(e)
        }

        state
            .build { scope.log(it) }
            ?.let { performAction(scope, coroutineScope, it) }
            ?: scope.log("not ready: $state")
    }

    private suspend fun performAction(
        scope: LegionScope<*>,
        coroutineScope: CoroutineScope,
        e: ImplPartialQuantified4Params<T1, T2, T3, T4>
    ) {
        scope.log("acc: $e to: $name -> ${links.map { it.name }}")
        action(e).let {
            links.forEach { link ->
                coroutineScope.launch {
                    link.accept(scope, this@launch, it)
                }
            }
        }
    }

    override fun <EventOther> LegionScope<*>.link(c: ImpNode<EOut, EventOther>): ImpNode<EOut, EventOther> {
        links.add(c)
        return c
    }

    override val uuid: UUID = UUID.randomUUID()
}

data class ImplPartialQuantified4Params<T1, T2, T3, T4>(
    val param1: List<T1>,
    val param2: List<T2>,
    val param3: List<T3>,
    val param4: List<T4>,
) {
    data class Builder<T1, T2, T3, T4, R>(
        private val quantity1: Int,
        private val quantity2: Int,
        private val quantity3: Int,
        private val quantity4: Int,
        private var param1: MutableList<T1> = mutableListOf(),
        private var param2: MutableList<T2> = mutableListOf(),
        private var param3: MutableList<T3> = mutableListOf(),
        private var param4: MutableList<T4> = mutableListOf()
    ) {
        private val mutex = Mutex()

        suspend fun withParam1(value: T1): Builder<T1, T2, T3, T4, R> = mutex.withLock {
            this.param1.add(value)
            return this
        }

        suspend fun withParam2(value: T2): Builder<T1, T2, T3, T4, R> = mutex.withLock {
            this.param2.add(value)
            return this
        }

        suspend fun withParam3(value: T3): Builder<T1, T2, T3, T4, R> = mutex.withLock {
            this.param3.add(value)
            return this
        }

        suspend fun withParam4(value: T4): Builder<T1, T2, T3, T4, R> = mutex.withLock {
            this.param4.add(value)
            return this
        }

        suspend fun build(
            log: (message: String) -> Unit
        ): ImplPartialQuantified4Params<T1, T2, T3, T4>? = mutex.withLock {
            coroutineScope {
                // Ensure all parameters are provided before building
                try {
                    log("$param1: $param2: $param3: $param4")
                    val p1 = param1
                        .takeIf { it.size >= quantity1 }
                        ?.take(quantity1)
                        ?: throw IllegalStateException("Parameter 1 must be provided.")
                    val p2 = param2
                        .takeIf { it.size >= quantity2 }
                        ?.take(quantity2)
                        ?: throw IllegalStateException("Parameter 2 must be provided.")
                    val p3 = param3
                        .takeIf { it.size >= quantity3 }
                        ?.take(quantity3)
                        ?: throw IllegalStateException("Parameter 3 must be provided.")
                    val p4 = param4
                        .takeIf { it.size >= quantity4 }
                        ?.take(quantity4)
                        ?: throw IllegalStateException("Parameter 4 must be provided.")

                    consume(log)

                    // Return the fully applied function
                    ImplPartialQuantified4Params(p1, p2, p3, p4)
                } catch (e: IllegalStateException) {
                    log(e.message.toString())
                    return@coroutineScope null
                }
            }
        }

        private fun consume(log: (message: String) -> Unit) {
            param1 = param1.drop(quantity1).toMutableList()
            param2 = param2.drop(quantity2).toMutableList()
            param3 = param3.drop(quantity3).toMutableList()
            param4 = param4.drop(quantity4).toMutableList()
            log("Consumed, remaining: $this")
        }
    }
}