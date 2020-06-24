package de.mkammerer.grpcchat.server

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A room.
 */
data class Room(val name: String)

/**
 * Is thrown if a room already exists.
 */
class RoomAlreadyExistsException(name: String) : Exception("Room '$name' already exist")

/**
 * Manages rooms.
 */
interface RoomService {
    /**
     * Creates a new room with the given [name] and the given [user] as owner.
     */
    fun create(user: User, name: String): Room

    /**
     * Determines if the room with the given [name] exists.
     */
    fun exists(name: String): Boolean

    /**
     * Joins the given [room] with the given [user].
     */
    fun join(user: User, room: Room)

    /**
     * Leaves the given [room] with the given [user].
     */
    fun leave(user: User, room: Room)

    /**
     * Lists all rooms.
     */
    fun all(): Set<Room>

    /**
     * Finds the room with the given [name].
     */
    fun find(name: String): Room?

    /**
     * Lists all rooms where [user] is in.
     */
    fun listUserRooms(user: User): Set<Room>

    /**
     * Lists users in the given [room].
     */
    fun listUsers(room: Room): Set<User>
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
        rooms[room.name] = room

        join(user, room)
        return room
    }

    override fun exists(name: String): Boolean {
        return rooms.containsKey(name)
    }

    override fun listUserRooms(user: User): Set<Room> {
        return members.filterValues { it.contains(user) }.keys
    }

    override fun listUsers(room: Room): Set<User> {
        return members[room] ?: setOf()
    }
}