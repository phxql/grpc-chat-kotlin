package de.mkammerer.grpcchat.server

import java.security.SecureRandom
import java.util.*

/**
 * Generates tokens.
 */
interface TokenGenerator {
    /**
     * Generates a new token.
     */
    fun create(): String
}

object TokenGeneratorImpl : TokenGenerator {
    private const val TOKEN_SIZE = 16
    private val secureRandom = SecureRandom()

    override fun create(): String {
        val bytes = ByteArray(TOKEN_SIZE)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}