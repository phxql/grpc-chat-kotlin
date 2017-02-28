package de.mkammerer.grpcchat.server

import java.time.Instant

/**
 * A clock.
 */
interface Clock {
    /**
     * Returns the current instant.
     */
    fun now(): Instant
}

/**
 * Real time clock.
 */
object WallClock : Clock {
    override fun now(): Instant = Instant.now()
}