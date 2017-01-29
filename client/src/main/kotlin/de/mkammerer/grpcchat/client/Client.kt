package de.mkammerer.grpcchat.client

import de.mkammerer.grpcchat.protocol.ChatGrpc
import de.mkammerer.grpcchat.protocol.LoginRequest
import de.mkammerer.grpcchat.protocol.RegisterRequest
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory

const val USERNAME = "moe"
const val PASSWORD = "eomeom"

fun main(args: Array<String>) {
    Client(USERNAME, PASSWORD).start()
}

class Client(private val username: String, private val password: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val connector: ChatGrpc.ChatBlockingStub

    init {
        val channel = ManagedChannelBuilder.forAddress("localhost", 5001)
                .usePlaintext(true)
                .build()
        connector = ChatGrpc.newBlockingStub(channel)
    }

    fun start() {
        register()
        login()
    }

    private fun register() {
        val request = RegisterRequest.newBuilder().setUsername(username).setPassword(password).build()
        val response = connector.register(request)

        if (response.registered) {
            logger.info("Register successful")
        } else {
            logger.info("Register failed")
        }
    }

    private fun login() {
        val request = LoginRequest.newBuilder().setUsername(username).setPassword(password).build()
        val response = connector.login(request)

        if (response.loggedIn) {
            val token = response.token
            logger.info("Login successful, token is $token")
        } else {
            logger.info("Login failed")
        }
    }
}