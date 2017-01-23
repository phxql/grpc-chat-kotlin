package de.mkammerer.grpcchat.client

import de.mkammerer.grpcchat.protocol.GreeterGrpc
import de.mkammerer.grpcchat.protocol.HelloRequest
import io.grpc.ManagedChannelBuilder

fun main(args: Array<String>) {
    val channel = ManagedChannelBuilder.forAddress("localhost", 5001)
            .usePlaintext(true)
            .build()
    val blockingStub = GreeterGrpc.newBlockingStub(channel)

    val request = HelloRequest.newBuilder().setName("Client 1").build()
    val response = blockingStub.sayHello(request)

    println("Got response: ${response.message}")
}