package de.mkammerer.grpcchat.server

import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class Message(val timestamp: Instant, val room: Room, val user: User, val text: String)

interface Subscription {
    fun cancel()
}

interface MessageService {
    fun register(user: User, callback: (Message) -> Unit): Subscription

    fun unregister(user: User)

    fun send(room: Room, user: User, text: String)
}

class InMemoryMessageService(private val clock: Clock) : MessageService {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val subscriber = ConcurrentHashMap<User, (Message) -> Unit>()

    override fun register(user: User, callback: (Message) -> Unit): Subscription {
        subscriber.put(user, callback)

        return object : Subscription {
            override fun cancel() {
                unregister(user)
            }
        }
    }

    override fun unregister(user: User) {
        subscriber.remove(user)
    }

    override fun send(room: Room, user: User, text: String) {
        val message = Message(clock.now(), room, user, text)
        subscriber.entries.forEach {
            try {
                it.value(message)
            } catch(ex: Exception) {
                logger.warn("Subscriber {} threw exception, removing it", it.key)
                subscriber.remove(it.key)
            }
        }
    }
}