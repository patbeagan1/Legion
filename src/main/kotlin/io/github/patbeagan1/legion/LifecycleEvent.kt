package io.github.patbeagan1.legion

/**
 * Represents events that occur during the lifecycle of nodes in the Legion processing graph.
 */
sealed class LifecycleEvent {
    abstract val name: String
    abstract val timeStamp: Long
    abstract val threadName: String

    data class New(
        override val name: String,
        override val timeStamp: Long = System.currentTimeMillis(),
        override val threadName: String
    ) : LifecycleEvent()

    data class Ready(
        override val name: String,
        override val threadName: String,
        override val timeStamp: Long = System.currentTimeMillis()
    ) : LifecycleEvent()

    data class Running(
        override val name: String,
        override val threadName: String,
        override val timeStamp: Long = System.currentTimeMillis()
    ) : LifecycleEvent()

    data class Waiting(
        override val name: String,
        override val threadName: String,
        override val timeStamp: Long = System.currentTimeMillis()
    ) : LifecycleEvent()

    data class Terminated(
        override val name: String,
        override val threadName: String,
        override val timeStamp: Long = System.currentTimeMillis()
    ) : LifecycleEvent()

    data class Transmitting(
        override val name: String,
        val target: String,
        override val threadName: String,
        override val timeStamp: Long = System.currentTimeMillis()
    ) : LifecycleEvent()
}