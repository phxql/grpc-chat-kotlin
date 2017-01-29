package de.mkammerer.grpcchat.server

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class Room(val name: String)

class RoomAlreadyExistsException(name: String) : Exception("Room '$name' already exist")
class RoomNotFoundException(name: String) : Exception("Room '$name' not found")

interface RoomService {
    fun create(name: String): Room

    fun exists(name: String): Boolean

    fun join(user: User, room: Room)

    fun all(): List<Room>
}

object InMemoryRoomService : RoomService {
    private val rooms = ConcurrentHashMap<String, Room>()

    private val members = ConcurrentHashMap<Room, MutableList<User>>()

    override fun join(user: User, room: Room) {
        members.getOrPut(room, { CopyOnWriteArrayList<User>() }).add(user)
    }

    override fun all(): List<Room> {
        return rooms.values.toList()
    }

    override fun create(name: String): Room {
        if (exists(name)) throw RoomAlreadyExistsException(name)

        val room = Room(name)
        rooms.put(room.name, room)
        return room
    }

    override fun exists(name: String): Boolean {
        return rooms.containsKey(name)
    }

}