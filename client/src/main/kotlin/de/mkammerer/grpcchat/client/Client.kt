package de.mkammerer.grpcchat.client

import de.mkammerer.grpcchat.protocol.*
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory

const val USERNAME = "moe"
const val PASSWORD = "eomeom"

fun main(args: Array<String>) {
    Client(USERNAME, PASSWORD).start()
}

class TokenMissingException : Exception("Token is missing. Call login() first")

class Client(private val username: String, private val password: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val connector: ChatGrpc.ChatBlockingStub
    private var token: String? = null

    init {
        val channel = ManagedChannelBuilder.forAddress("localhost", 5001)
                .usePlaintext(true)
                .build()
        connector = ChatGrpc.newBlockingStub(channel)
    }

    fun start() {
        register()
        login()
        createRoom("Room #1")
        listRooms()
        listUserRooms()
        createRoom("Room #2")
        listRooms()
        listUserRooms()
        leaveRoom("Room #1")
        listRooms()
        listUserRooms()
    }

    private fun leaveRoom(name: String) {
        if (token == null) throw TokenMissingException()

        val request = LeaveRoomRequest.newBuilder().setToken(token).setName(name).build()
        val response = connector.leaveRoom(request)

        if (response.left) {
            logger.info("Room left")
        } else {
            logger.info("Room leave failed, error: {}", response.error)
        }
    }

    private fun createRoom(name: String) {
        if (token == null) throw TokenMissingException()

        val request = CreateRoomRequest.newBuilder().setToken(token).setName(name).build()
        val response = connector.createRoom(request)

        if (response.created) {
            logger.info("Room created")
        } else {
            logger.info("Room creation failed, error: {}", response.error)
        }
    }

    private fun register() {
        val request = RegisterRequest.newBuilder().setUsername(username).setPassword(password).build()
        val response = connector.register(request)

        if (response.registered) {
            logger.info("Register successful")
        } else {
            logger.info("Register failed, error: {}", response.error)
        }
    }

    private fun login() {
        val request = LoginRequest.newBuilder().setUsername(username).setPassword(password).build()
        val response = connector.login(request)

        if (response.loggedIn) {
            token = response.token
            logger.info("Login successful, token is $token")
        } else {
            logger.info("Login failed, error: {}", response.error)
        }
    }

    private fun listRooms() {
        if (token == null) throw TokenMissingException()

        val request = ListRoomsRequest.newBuilder().setToken(token).build()
        val response = connector.listRooms(request)

        if (response.error.code == Codes.SUCCESS) {
            logger.info("Rooms on server:")
            response.roomsList.forEach { it -> logger.info(it) }
        } else {
            logger.info("List rooms failed, error: {}", response.error)
        }
    }

    private fun listUserRooms() {
        if (token == null) throw TokenMissingException()

        val request = ListRoomsRequest.newBuilder().setToken(token).build()
        val response = connector.listUserRooms(request)

        if (response.error.code == Codes.SUCCESS) {
            logger.info("Rooms:")
            response.roomsList.forEach { it -> logger.info(it) }
        } else {
            logger.info("List user rooms failed, error: {}", response.error)
        }
    }
}