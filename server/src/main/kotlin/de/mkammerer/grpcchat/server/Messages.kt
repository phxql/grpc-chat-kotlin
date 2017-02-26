package de.mkammerer.grpcchat.server

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

data class Message(val timestamp: Instant, val room: Room, val user: User, val text: String)

interface MessageService {
    fun register(callback: (Message) -> Unit)

    fun send(room: Room, user: User, text: String)
}

class InMemoryMessageService(private val clock: Clock) : MessageService {
    private val subscriber = CopyOnWriteArrayList<(Message) -> Unit>()

    override fun register(callback: (Message) -> Unit) {
        subscriber.add(callback)
    }

    override fun send(room: Room, user: User, text: String) {
        val message = Message(clock.now(), room, user, text)
        subscriber.forEach { it(message) }
    }
}