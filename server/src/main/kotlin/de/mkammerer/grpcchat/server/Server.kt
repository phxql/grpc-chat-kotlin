package de.mkammerer.grpcchat.server

import de.mkammerer.grpcchat.protocol.GreeterGrpc
import de.mkammerer.grpcchat.protocol.HelloReply
import de.mkammerer.grpcchat.protocol.HelloRequest
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver

fun main(args: Array<String>) {
    val server = ServerBuilder.forPort(5001)
            .addService(GreeterImpl)
            .build()
            .start()
    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            server.shutdown()
        }
    })
    server.awaitTermination()
}

object GreeterImpl : GreeterGrpc.GreeterImplBase() {
    override fun sayHello(req: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
        val reply = HelloReply.newBuilder().setMessage("Hello " + req.name).build()
        responseObserver.onNext(reply)
        responseObserver.onCompleted()
    }
}