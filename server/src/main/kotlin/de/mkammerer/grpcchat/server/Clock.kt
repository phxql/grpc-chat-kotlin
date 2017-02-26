package de.mkammerer.grpcchat.server

import java.time.Instant

interface Clock {
    fun now(): Instant
}

object WallClock : Clock {
    override fun now(): Instant = Instant.now()
}