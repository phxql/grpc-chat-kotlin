package de.mkammerer.grpcchat.server

import com.google.common.cache.CacheBuilder
import de.mkammerer.grpcchat.protocol.*
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    Server.start()
}

object Server {
    private const val PORT = 5001
    private val logger = LoggerFactory.getLogger(javaClass)

    fun start() {
        val userService = InMemoryUserService
        val tokenGenerator = TokenGeneratorImpl
        val chat = Chat(userService, tokenGenerator)

        val server = ServerBuilder.forPort(PORT).addService(chat).build().start()
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                server.shutdown()
            }
        })
        logger.info("Server running on port $PORT")
        server.awaitTermination()
    }
}

class Chat(
        private val userService: UserService,
        private val tokenGenerator: TokenGenerator
) : ChatGrpc.ChatImplBase() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val users = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build<String, User>()

    override fun register(request: RegisterRequest, responseObserver: StreamObserver<RegisterResponse>) {
        val reply = try {
            val user = userService.register(request.username, request.password)
            logger.info("User ${user.username} registered")
            RegisterResponse.newBuilder().setRegistered(true).build()
        } catch (ex: UserAlreadyExistsException) {
            RegisterResponse.newBuilder().setRegistered(false).build()
        }

        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }

    override fun login(request: LoginRequest, responseObserver: StreamObserver<LoginResponse>) {
        val user = userService.login(request.username, request.password)
        val reply = if (user != null) {
            val token = tokenGenerator.create()
            users.put(token, user)

            logger.info("User ${user.username} logged in. Access token is $token")
            LoginResponse.newBuilder().setLoggedIn(true).setToken(token).build()
        } else {
            LoginResponse.newBuilder().setLoggedIn(false).build()
        }

        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}