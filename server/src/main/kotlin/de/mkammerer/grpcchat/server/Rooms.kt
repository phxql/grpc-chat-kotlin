package de.mkammerer.grpcchat.server

import java.util.concurrent.ConcurrentHashMap

data class Room(val name: String)

class RoomAlreadyExistsException(name: String) : Exception("Room '$name' already exist")

interface RoomService {
    fun create(name: String): Room

    fun exists(name: String): Boolean
}

object InMemoryRoomService : RoomService {
    private val rooms = ConcurrentHashMap<String, Room>()

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