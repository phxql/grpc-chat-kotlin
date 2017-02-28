package de.mkammerer.grpcchat.server

import com.google.common.cache.CacheBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * A user.
 */
data class User(val username: String, val password: String) {
    override fun toString(): String = username
}

/**
 * A token.
 */
data class Token(val data: String) {
    override fun toString() = data
}

/**
 * Manages users.
 */
interface UserService {
    /**
     * Registeres a new user.
     *
     * @throws UserAlreadyExistsException If the user already exists.
     */
    fun register(username: String, password: String): User

    /**
     * Logs the user with the given [username] and [password] in. Returns an access token.
     */
    fun login(username: String, password: String): Token?

    /**
     * Validates the [token] and returns the corresponding user.
     */
    fun validateToken(token: Token): User?

    /**
     * Determines if the user with the given [username] exists.
     */
    fun exists(username: String): Boolean
}

/**
 * Is thrown if the user already exists.
 */
class UserAlreadyExistsException(username: String) : Exception("User '$username' already exists")

class InMemoryUserService(
        private val tokenGenerator: TokenGenerator
) : UserService {
    private val users = ConcurrentHashMap<String, User>()
    private val loggedIn = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build<Token, User>()

    override fun exists(username: String): Boolean {
        return users.containsKey(username)
    }

    override fun register(username: String, password: String): User {
        if (exists(username)) throw UserAlreadyExistsException(username)

        val user = User(username, password)
        users.put(user.username, user)
        return user
    }

    override fun login(username: String, password: String): Token? {
        val user = users[username] ?: return null

        if (user.password == password) {
            val token = Token(tokenGenerator.create())
            loggedIn.put(token, user)
            return token
        } else return null
    }

    override fun validateToken(token: Token): User? {
        return loggedIn.getIfPresent(token)
    }
}