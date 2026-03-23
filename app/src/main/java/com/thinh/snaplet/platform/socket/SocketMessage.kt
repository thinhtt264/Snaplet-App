package com.thinh.snaplet.platform.socket

data class SocketMessage(
    val event: SocketEvent,
    val args: String?
)
