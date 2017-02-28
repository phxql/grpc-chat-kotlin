package de.mkammerer.grpcchat.protocol

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

object JoinRoomCodes {
    const val ALREADY_IN_ROOM = 1
    const val ROOM_DOESNT_EXIST = 2
}

object LeaveRoomCodes {
    const val NOT_IN_ROOM = 1
    const val ROOM_DOESNT_EXIST = 2
}

object SendMessageCodes {
    const val NOT_IN_ROOM = 1
}

object ListUsersInRoomCodes {
    const val ROOM_DOESNT_EXIST = 1
}