package de.mkammerer.grpcchat.server

import java.util.concurrent.CopyOnWriteArraySet

data class User(val username: String, val password: String)

interface UserService {
    fun register(username: String, password: String): User

    fun login(username: String, password: String): User?

    fun exists(username: String): Boolean
}

class UserAlreadyExistsException(username: String) : Exception("User '$username' already exists")

object InMemoryUserService : UserService {
    private val users = CopyOnWriteArraySet<User>()

    override fun exists(username: String): Boolean {
        return users.any { it.username == username }
    }

    override fun register(username: String, password: String): User {
        if (exists(username)) throw UserAlreadyExistsException(username)

        val user = User(username, password)
        users.add(user)
        return user
    }

    override fun login(username: String, password: String): User? {
        return users.first { it.username == username && password == password }
    }
}