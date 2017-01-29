package de.mkammerer.grpcchat.server

import de.mkammerer.grpcchat.protocol.*
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    Server.start()
}

object Server {
    private const val PORT = 5001
    private val logger = LoggerFactory.getLogger(javaClass)

    fun start() {
        val tokenGenerator = TokenGeneratorImpl
        val userService = InMemoryUserService(tokenGenerator)
        val roomService = InMemoryRoomService
        val chat = Chat(userService, roomService)

        val server = ServerBuilder.forPort(PORT).addService(chat).build().start()
        Runtime.getRuntime().addShutdownHook(Thread({
            server.shutdown()
        }))
        logger.info("Server running on port {}", PORT)
        server.awaitTermination()
    }
}

class Chat(
        private val userService: UserService,
        private val roomService: RoomService
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
                val room = roomService.create(user, request.name)
                CreateRoomResponse.newBuilder().setCreated(true).build()
            } catch(ex: RoomAlreadyExistsException) {
                CreateRoomResponse.newBuilder().setCreated(false).setError(error(CreateRoomCodes.ROOM_ALREADY_EXISTS, "Room already exists")).build()
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
}