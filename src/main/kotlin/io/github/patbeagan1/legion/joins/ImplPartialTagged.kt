package io.github.patbeagan1.legion.joins

import io.github.patbeagan1.legion.ImpNode
import io.github.patbeagan1.legion.LegionScope
import io.github.patbeagan1.legion.visualization.LinkCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

inline fun <reified T> LegionScope<*>.containerize(tag: String): ImpNode<T, Container<T>> =
    imp("container-${tag}") { Container(it, tag, T::class.java) }

data class Container<T>(val value: T, val tag: String, val java: Class<T>)

@JvmInline
value class ContainerList(private val values: List<Container<*>>) {
    fun <T> findByTag(tag: String): Container<T>? = values.find { it.tag == tag }.let { it as? Container<T> }
}


inline fun <reified EOut : Any> impJoinAllTagged(
    nameIn: String,
    paramCount: Int,
    noinline action: suspend (ContainerList) -> EOut
): ImpNode<Container<out Any>, EOut> = object : ImpNode<Container<out Any>, EOut> {
    private val links = mutableListOf<ImpNode<EOut, *>>()
    private var state = mutableListOf<Container<*>>()
    override val uuid: UUID = UUID.randomUUID()
    override val name: String = "join-${nameIn}-$uuid"
    override var isSubgraphEnd: Boolean = false
    val mutex = Mutex()

    override fun collectLinks(subgraphStack: LinkCollection.SubgraphStack, linkCollection: LinkCollection) {
        linkCollection.recordLinkSoft(this to links)
    }

    override suspend fun accept(
        legionScope: LegionScope<*>,
        coroutineScope: CoroutineScope,
        e: Container<out Any>
    ): Unit =
        mutex.withLock {
            state.add(e)
            state
                .takeIf { it.size == paramCount }
                ?.let { ContainerList(state) }
                ?.let {
                    legionScope.log("acc: $it to: $name -> ${links.map { it.name }}")
                    action(it).let {
                        links.forEach { link ->
                            coroutineScope.launch {
                                link.accept(legionScope, this@launch, it)
                            }
                        }
                    }
                }
                ?: legionScope.log("not ready: $state")
        }

    override fun <EventOther> LegionScope<*>.link(c: ImpNode<EOut, EventOther>): ImpNode<EOut, EventOther> = c
        .also { links.add(it) }
}