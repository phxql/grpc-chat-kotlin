package de.mkammerer.grpcchat.client

import de.mkammerer.grpcchat.protocol.ChatGrpc
import de.mkammerer.grpcchat.protocol.Codes
import de.mkammerer.grpcchat.protocol.CreateRoomRequest
import de.mkammerer.grpcchat.protocol.GetMessagesRequest
import de.mkammerer.grpcchat.protocol.JoinRoomRequest
import de.mkammerer.grpcchat.protocol.LeaveRoomRequest
import de.mkammerer.grpcchat.protocol.ListRoomsRequest
import de.mkammerer.grpcchat.protocol.ListUsersInRoomRequest
import de.mkammerer.grpcchat.protocol.LoginRequest
import de.mkammerer.grpcchat.protocol.RegisterRequest
import de.mkammerer.grpcchat.protocol.SendMessageRequest
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

fun main(args: Array<String>) {
    // Use the first argument as username. If no arguments are given, generate a unique username
    val username = if (args.isEmpty()) UUID.randomUUID().toString() else args[0]

    Client(username, username.reversed()).start()
}

class TokenMissingException : Exception("Token is missing. Call login() first")

class Client(private val username: String, private val password: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val connector: ChatGrpc.ChatBlockingStub
    private var token: String? = null

    init {
        val channel = ManagedChannelBuilder.forAddress("localhost", 5001)
                .usePlaintext()
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

        joinRoom("Room #2")

        listUsersInRoom("Room #1")
        listUsersInRoom("Room #2")

        val thread = Thread({
            var i = 0
            while (true) {
                i++
                sendMessage("Room #2", "Message #$i")
                Thread.sleep(2000)
            }
        }, "Message sender")
        thread.isDaemon = true
        thread.start()

        getMessages()
    }

    private fun joinRoom(name: String) {
        if (token == null) throw TokenMissingException()

        val request = JoinRoomRequest.newBuilder().setToken(token).setName(name).build()
        val response = connector.joinRoom(request)

        if (response.joined) {
            logger.info("Room joined")
        } else {
            logger.error("Join room failed, error: {}", response.error)
        }
    }

    private fun getMessages() {
        if (token == null) throw TokenMissingException()

        val request = GetMessagesRequest.newBuilder().setToken(token).build()
        logger.info("Receiving messages ...")
        connector.getMessages(request).forEach { value ->
            if (value.error.code == Codes.SUCCESS) {
                val sent = Instant.ofEpochMilli(value.timestamp)
                logger.info("Message in room {} from {}, sent {}: {}", value.room, value.from, sent, value.text)
            } else {
                logger.error("Receiving messages failed, error: {}", value.error)
            }
        }
    }

    private fun listUsersInRoom(room: String) {
        if (token == null) throw TokenMissingException()

        val request = ListUsersInRoomRequest.newBuilder().setToken(token).setName(room).build()
        val response = connector.listUsersInRoom(request)

        if (response.error.code == Codes.SUCCESS) {
            logger.info("Users in room {}:", room)
            response.usersList.forEach {
                logger.info("\t{}", it)
            }
        } else {
            logger.error("Listing users in room failed, error: {}", response.error)
        }
    }

    private fun sendMessage(room: String, text: String) {
        if (token == null) throw TokenMissingException()

        val request = SendMessageRequest.newBuilder().setToken(token).setRoom(room).setText(text).build()
        val response = connector.sendMessage(request)

        if (response.sent) {
            logger.info("Message sent")
        } else {
            logger.error("Send message failed, error: {}", response.error)
        }
    }

    private fun leaveRoom(name: String) {
        if (token == null) throw TokenMissingException()

        val request = LeaveRoomRequest.newBuilder().setToken(token).setName(name).build()
        val response = connector.leaveRoom(request)

        if (response.left) {
            logger.info("Room left")
        } else {
            logger.error("Leave room failed, error: {}", response.error)
        }
    }

    private fun createRoom(name: String) {
        if (token == null) throw TokenMissingException()

        val request = CreateRoomRequest.newBuilder().setToken(token).setName(name).build()
        val response = connector.createRoom(request)

        if (response.created) {
            logger.info("Room created")
        } else {
            logger.error("Room creation failed, error: {}", response.error)
        }
    }

    private fun register() {
        val request = RegisterRequest.newBuilder().setUsername(username).setPassword(password).build()
        val response = connector.register(request)

        if (response.registered) {
            logger.info("Register successful")
        } else {
            logger.error("Register failed, error: {}", response.error)
        }
    }

    private fun login() {
        val request = LoginRequest.newBuilder().setUsername(username).setPassword(password).build()
        val response = connector.login(request)

        if (response.loggedIn) {
            token = response.token
            logger.info("Login successful, token is $token")
        } else {
            logger.error("Login failed, error: {}", response.error)
        }
    }

    private fun listRooms() {
        if (token == null) throw TokenMissingException()

        val request = ListRoomsRequest.newBuilder().setToken(token).build()
        val response = connector.listRooms(request)

        if (response.error.code == Codes.SUCCESS) {
            logger.info("Rooms on server:")
            response.roomsList.forEach { logger.info("\t{}", it) }
        } else {
            logger.error("List rooms failed, error: {}", response.error)
        }
    }

    private fun listUserRooms() {
        if (token == null) throw TokenMissingException()

        val request = ListRoomsRequest.newBuilder().setToken(token).build()
        val response = connector.listUserRooms(request)

        if (response.error.code == Codes.SUCCESS) {
            logger.info("Rooms:")
            response.roomsList.forEach { logger.info("\t{}", it) }
        } else {
            logger.error("List user rooms failed, error: {}", response.error)
        }
    }
}