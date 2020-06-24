package de.mkammerer.grpcchat.server

import de.mkammerer.grpcchat.protocol.ChatGrpc
import de.mkammerer.grpcchat.protocol.Codes
import de.mkammerer.grpcchat.protocol.CreateRoomCodes
import de.mkammerer.grpcchat.protocol.CreateRoomRequest
import de.mkammerer.grpcchat.protocol.CreateRoomResponse
import de.mkammerer.grpcchat.protocol.Error
import de.mkammerer.grpcchat.protocol.GetMessagesRequest
import de.mkammerer.grpcchat.protocol.GetMessagesResponse
import de.mkammerer.grpcchat.protocol.JoinRoomCodes
import de.mkammerer.grpcchat.protocol.JoinRoomRequest
import de.mkammerer.grpcchat.protocol.JoinRoomResponse
import de.mkammerer.grpcchat.protocol.LeaveRoomCodes
import de.mkammerer.grpcchat.protocol.LeaveRoomRequest
import de.mkammerer.grpcchat.protocol.LeaveRoomResponse
import de.mkammerer.grpcchat.protocol.ListRoomsRequest
import de.mkammerer.grpcchat.protocol.ListRoomsResponse
import de.mkammerer.grpcchat.protocol.ListUsersInRoomCodes
import de.mkammerer.grpcchat.protocol.ListUsersInRoomRequest
import de.mkammerer.grpcchat.protocol.ListUsersInRoomResponse
import de.mkammerer.grpcchat.protocol.LoginCodes
import de.mkammerer.grpcchat.protocol.LoginRequest
import de.mkammerer.grpcchat.protocol.LoginResponse
import de.mkammerer.grpcchat.protocol.RegisterCodes
import de.mkammerer.grpcchat.protocol.RegisterRequest
import de.mkammerer.grpcchat.protocol.RegisterResponse
import de.mkammerer.grpcchat.protocol.SendMessageCodes
import de.mkammerer.grpcchat.protocol.SendMessageRequest
import de.mkammerer.grpcchat.protocol.SendMessageResponse
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory

fun main() {
    Server.start()
}

object Server {
    private const val PORT = 5001
    private val logger = LoggerFactory.getLogger(javaClass)

    fun start() {
        val tokenGenerator = TokenGeneratorImpl
        val userService = InMemoryUserService(tokenGenerator)
        val roomService = InMemoryRoomService
        val messageService = InMemoryMessageService(WallClock)
        val chat = Chat(userService, roomService, messageService)

        val server = ServerBuilder.forPort(PORT)
                // this will disable thread pools and execute everything on the event loop. DO NOT BLOCK in any implemented
                // ImplBase methods!
                .directExecutor()
                .addService(chat)
                .build().start()
        Runtime.getRuntime().addShutdownHook(Thread {
            server.shutdown()
        })
        logger.info("Server running on port {}", PORT)
        server.awaitTermination()
    }
}

class Chat(
        private val userService: UserService,
        private val roomService: RoomService,
        private val messageService: MessageService
) : ChatGrpc.ChatImplBase() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun error(code: Int, message: String): Error {
        return Error.newBuilder().setCode(code).setMessage(message).build()
    }

    override fun login(request: LoginRequest, responseObserver: StreamObserver<LoginResponse>) {
        val token = userService.login(request.username, request.password)
        val response = if (token != null) {
            logger.info("User {} logged in. Access token is {}", request.username, token)
            LoginResponse.newBuilder().setLoggedIn(true).setToken(token.data).build()
        } else {
            LoginResponse.newBuilder().setLoggedIn(false).setError(error(LoginCodes.INVALID_CREDENTIALS, "Invalid credentials")).build()
        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun register(request: RegisterRequest, responseObserver: StreamObserver<RegisterResponse>) {
        val response = try {
            val user = userService.register(request.username, request.password)
            logger.info("User {} registered", user.username)
            RegisterResponse.newBuilder().setRegistered(true).build()
        } catch (ex: UserAlreadyExistsException) {
            RegisterResponse.newBuilder().setRegistered(false).setError(error(RegisterCodes.USERNAME_ALREADY_EXISTS, "Username already exists")).build()
        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun createRoom(request: CreateRoomRequest, responseObserver: StreamObserver<CreateRoomResponse>) {
        val user = userService.validateToken(Token(request.token))

        val response = if (user == null) {
            CreateRoomResponse.newBuilder().setError(error(Codes.INVALID_TOKEN, "Invalid token")).setCreated(false).build()
        } else {
            try {
                roomService.create(user, request.name)
                CreateRoomResponse.newBuilder().setCreated(true).build()
            } catch(ex: RoomAlreadyExistsException) {
                CreateRoomResponse.newBuilder().setCreated(false).setError(error(CreateRoomCodes.ROOM_ALREADY_EXISTS, "Room already exists")).build()
            }
        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun joinRoom(request: JoinRoomRequest, responseObserver: StreamObserver<JoinRoomResponse>) {
        val user = userService.validateToken(Token(request.token))

        val response = if (user == null) {
            JoinRoomResponse.newBuilder().setError(error(Codes.INVALID_TOKEN, "Invalid token")).setJoined(false).build()
        } else {
            val userRooms = roomService.listUserRooms(user)
            // Check if already in room
            if (userRooms.any { r -> r.name == request.name }) {
                JoinRoomResponse.newBuilder().setError(error(JoinRoomCodes.ALREADY_IN_ROOM, "Already in room")).setJoined(false).build()
            } else {
                val room = roomService.find(request.name)
                // Check if room exists
                if (room == null) {
                    JoinRoomResponse.newBuilder().setError(error(JoinRoomCodes.ROOM_DOESNT_EXIST, "Room doesn't exist")).setJoined(false).build()
                } else {
                    roomService.join(user, room)
                    JoinRoomResponse.newBuilder().setJoined(true).build()
                }
            }
        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun leaveRoom(request: LeaveRoomRequest, responseObserver: StreamObserver<LeaveRoomResponse>) {
        val user = userService.validateToken(Token(request.token))

        val response = if (user == null) {
            LeaveRoomResponse.newBuilder().setError(error(Codes.INVALID_TOKEN, "Invalid token")).setLeft(false).build()
        } else {
            val userRooms = roomService.listUserRooms(user)
            // Check if already in room
            if (userRooms.none { r -> r.name == request.name }) {
                LeaveRoomResponse.newBuilder().setError(error(LeaveRoomCodes.NOT_IN_ROOM, "Not in room")).setLeft(false).build()
            } else {
                val room = roomService.find(request.name)
                // Check if room exists
                if (room == null) {
                    LeaveRoomResponse.newBuilder().setError(error(LeaveRoomCodes.ROOM_DOESNT_EXIST, "Room doesn't exist")).setLeft(false).build()
                } else {
                    roomService.leave(user, room)
                    LeaveRoomResponse.newBuilder().setLeft(true).build()
                }
            }
        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun listRooms(request: ListRoomsRequest, responseObserver: StreamObserver<ListRoomsResponse>) {
        val user = userService.validateToken(Token(request.token))

        val response = if (user == null) {
            ListRoomsResponse.newBuilder().setError(error(Codes.INVALID_TOKEN, "Invalid token")).build()
        } else {
            val rooms = roomService.all()
            ListRoomsResponse.newBuilder().addAllRooms(rooms.map(Room::name)).build()

        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun listUserRooms(request: ListRoomsRequest, responseObserver: StreamObserver<ListRoomsResponse>) {
        val user = userService.validateToken(Token(request.token))

        val response = if (user == null) {
            ListRoomsResponse.newBuilder().setError(error(Codes.INVALID_TOKEN, "Invalid token")).build()
        } else {
            val rooms = roomService.listUserRooms(user)
            ListRoomsResponse.newBuilder().addAllRooms(rooms.map(Room::name)).build()

        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getMessages(request: GetMessagesRequest, responseObserver: StreamObserver<GetMessagesResponse>) {
        val user = userService.validateToken(Token(request.token))

        if (user == null) {
            val response = GetMessagesResponse.newBuilder().setError(error(Codes.INVALID_TOKEN, "Invalid token")).build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } else {
            logger.info("Registering user {} for messages", user.username)
            messageService.register(user) { message ->
                // Only distribute message if user is in room and some other user sent the message
                if (roomService.listUserRooms(user).contains(message.room) && user != message.user) {
                    val response = GetMessagesResponse.newBuilder()
                            .setFrom(message.user.username)
                            .setRoom(message.room.name)
                            .setText(message.text)
                            .setTimestamp(message.timestamp.toEpochMilli())
                            .build()
                    responseObserver.onNext(response)
                }
            }
        }
    }

    override fun sendMessage(request: SendMessageRequest, responseObserver: StreamObserver<SendMessageResponse>) {
        val user = userService.validateToken(Token(request.token))

        val response = if (user == null) {
            SendMessageResponse.newBuilder().setError(error(Codes.INVALID_TOKEN, "Invalid token")).build()
        } else {
            val rooms = roomService.listUserRooms(user)

            // Check if already in room
            val room = rooms.find { r -> r.name == request.room }
            if (room == null) {
                SendMessageResponse.newBuilder().setError(error(SendMessageCodes.NOT_IN_ROOM, "Not in room")).setSent(false).build()
            } else {
                messageService.send(room, user, request.text)
                SendMessageResponse.newBuilder().setSent(true).build()
            }
        }

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun listUsersInRoom(request: ListUsersInRoomRequest, responseObserver: StreamObserver<ListUsersInRoomResponse>) {
        val user = userService.validateToken(Token(request.token))

        val response = if (user == null) {
            ListUsersInRoomResponse.newBuilder().setError(error(Codes.INVALID_TOKEN, "Invalid token")).build()
        } else {
            val room = roomService.find(request.name)

            if (room == null) {
                ListUsersInRoomResponse.newBuilder().setError(error(ListUsersInRoomCodes.ROOM_DOESNT_EXIST, "Room doens't exist")).build()
            } else {
                val users = roomService.listUsers(room)
                ListUsersInRoomResponse.newBuilder().addAllUsers(users.map(User::username)).build()
            }
        }
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}