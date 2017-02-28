package de.mkammerer.grpcchat.client

import de.mkammerer.grpcchat.protocol.*
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.time.Instant

fun main(args: Array<String>) {
    Client(args[0], args[0]).start()
}

class TokenMissingException : Exception("Token is missing. Call login() first")

class Client(private val username: String, private val password: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val connector: ChatGrpc.ChatBlockingStub
    private val asyncConnector: ChatGrpc.ChatStub
    private var token: String? = null

    init {
        val channel = ManagedChannelBuilder.forAddress("localhost", 5001)
                .usePlaintext(true)
                .build()
        connector = ChatGrpc.newBlockingStub(channel)
        asyncConnector = ChatGrpc.newStub(channel)
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

        getMessages()

        Thread.sleep(500)

        joinRoom("Room #2")

        Thread({
            var i = 0
            while (true) {
                i++
                sendMessage("Room #2", "Message #$i")
                Thread.sleep(2000)
            }
        }, "Message sender").start()

        Thread.sleep(Long.MAX_VALUE)
    }

    private fun joinRoom(name: String) {
        if (token == null) throw TokenMissingException()

        val request = JoinRoomRequest.newBuilder().setToken(token).setName(name).build()
        val response = connector.joinRoom(request)

        if (response.joined) {
            logger.info("Room joined")
        } else {
            logger.info("Join room failed, error: {}", response.error)
        }
    }

    private fun getMessages() {
        if (token == null) throw TokenMissingException()

        val request = GetMessagesRequest.newBuilder().setToken(token).build()
        logger.info("Receiving messages ...")
        asyncConnector.getMessages(request, object : StreamObserver<GetMessagesResponse> {
            override fun onCompleted() {
                logger.info("No more messages")
            }

            override fun onError(t: Throwable) {
                logger.error("Error receiving messages: {}", t.message)
            }

            override fun onNext(value: GetMessagesResponse) {
                if (value.error.code != 0) {
                    logger.info("Receiving messages failed, error: {}", value.error)
                } else {
                    val sent = Instant.ofEpochMilli(value.timestamp)
                    logger.info("Message in room {} from {}, sent {}: {}", value.room, value.from, sent, value.text)
                }
            }
        })
    }

    private fun sendMessage(room: String, text: String) {
        if (token == null) throw TokenMissingException()

        val request = SendMessageRequest.newBuilder().setToken(token).setRoom(room).setText(text).build()
        val response = connector.sendMessage(request)

        if (response.sent) {
            logger.info("Message sent")
        } else {
            logger.info("Send message failed, error: {}", response.error)
        }
    }

    private fun leaveRoom(name: String) {
        if (token == null) throw TokenMissingException()

        val request = LeaveRoomRequest.newBuilder().setToken(token).setName(name).build()
        val response = connector.leaveRoom(request)

        if (response.left) {
            logger.info("Room left")
        } else {
            logger.info("Leave room failed, error: {}", response.error)
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