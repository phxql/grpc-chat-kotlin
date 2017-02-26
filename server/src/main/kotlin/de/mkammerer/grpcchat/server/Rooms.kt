package de.mkammerer.grpcchat.server

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

data class Room(val name: String)

class RoomAlreadyExistsException(name: String) : Exception("Room '$name' already exist")
class RoomNotFoundException(name: String) : Exception("Room '$name' not found")

interface RoomService {
    fun create(user: User, name: String): Room

    fun exists(name: String): Boolean

    fun join(user: User, room: Room)

    fun leave(user: User, room: Room)

    fun all(): Set<Room>

    fun find(name: String): Room?

    /**
     * Lists all rooms where [user] is in.
     */
    fun listUserRooms(user: User): Set<Room>
}

object InMemoryRoomService : RoomService {
    private val rooms = ConcurrentHashMap<String, Room>()

    private val members = ConcurrentHashMap<Room, MutableSet<User>>()

    override fun join(user: User, room: Room) {
        members.getOrPut(room, { CopyOnWriteArraySet<User>() }).add(user)
    }

    override fun leave(user: User, room: Room) {
        members.getOrDefault(room, mutableSetOf()).remove(user)
    }

    override fun all(): Set<Room> {
        return rooms.values.toSet()
    }

    override fun find(name: String): Room? {
        return rooms[name]
    }

    override fun create(user: User, name: String): Room {
        if (exists(name)) throw RoomAlreadyExistsException(name)

        val room = Room(name)
        rooms.put(room.name, room)

        join(user, room)
        return room
    }

    override fun exists(name: String): Boolean {
        return rooms.containsKey(name)
    }

    override fun listUserRooms(user: User): Set<Room> {
        return members.filterValues { it.contains(user) }.keys
    }
}