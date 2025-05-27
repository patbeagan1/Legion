package io.github.patbeagan1.legion

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageHandler(val log: (String, LegionScope.DebugLevel) -> Unit) {
    private val mutex = Mutex()
    private val awaitMessageMap: MutableMap<String, MutableList<(LegionScope.Message) -> Unit>> = mutableMapOf()
    private val queuedMessages: MutableMap<String, LegionScope.Message> = mutableMapOf()

    suspend fun <T> awaitMessage(messageKey: String): T = mutex
        .withLock {
            log("await-start ($messageKey): $awaitMessageMap", LegionScope.DebugLevel.INFO)
            val v = awaitMessageMap.getOrPut(messageKey, { mutableListOf() })
            v
        }
        .let {
            suspendCoroutine { continuation ->
                val element: (LegionScope.Message) -> Unit = { continuation.resume(it as T) }
                it.add(element)
                log("await-end-- ($messageKey): $awaitMessageMap", LegionScope.DebugLevel.INFO)
            }
        }

    suspend fun <T : LegionScope.Message> awaitMessageQueued(messageKey: String): T {
//        val message2 = queuedMessages[messageKey]
//        if (message2 != null) {
//            queuedMessages.remove(messageKey)
//            return message2 as T
//        } else {
//
//        }
//
//        message2?.let { message ->
//
//
//        }
//        val message1 = message ?: awaitMessage(messageKey)
//        return message1
//            .let { it as T }
        throw Exception()
    }

    suspend fun <T : LegionScope.Message> sendMessage(messageKey: String, message: T): Unit = mutex.withLock {
        log("send-start ($messageKey): $awaitMessageMap", LegionScope.DebugLevel.INFO)
        awaitMessageMap[messageKey]?.let { listOfFunc ->
            listOfFunc.forEach { it.invoke(message) }
            awaitMessageMap.remove(messageKey)
        }
        log("send-end-- ($messageKey): $awaitMessageMap", LegionScope.DebugLevel.INFO)
    }

    suspend fun <T : LegionScope.Message> sendMessageQueued(messageKey: String, message: T): Unit = mutex.withLock {
        queuedMessages[messageKey] = message
        awaitMessageMap[messageKey]?.let { listOfFunc ->
            listOfFunc.forEach { it.invoke(message) }
            awaitMessageMap.remove(messageKey)
            queuedMessages.remove(messageKey)
        }
    }
}