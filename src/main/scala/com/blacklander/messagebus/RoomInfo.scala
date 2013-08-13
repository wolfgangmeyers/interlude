package com.blacklander.messagebus

case class RoomInfo(
    roomId: String,
    createdBy: String,
    inviteOnly: Boolean,
    isVisible: Boolean
)
