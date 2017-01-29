package de.mkammerer.grpcchat.server

object Codes {
    const val SUCCESS = 0
    const val INVALID_TOKEN = 999
}

object LoginCodes {
    const val INVALID_CREDENTIALS = 1
}

object RegisterCodes {
    const val USERNAME_ALREADY_EXISTS = 1
}

object CreateRoomCodes {
    const val ROOM_ALREADY_EXISTS = 1
}