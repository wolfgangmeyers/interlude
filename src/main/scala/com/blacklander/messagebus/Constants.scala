package com.blacklander.messagebus

object Constants {
	val MessageType = "messageType"
	val UserName = "userName"
	val Password = "password"
	val Message = "message"
	val Recipient = "recipient"
	val Anonymous = "_anonymous_"
	val GameId = "gameId"
	val RoomId = "roomId"
	
    object Messages {
	    val Hello = "hello"
		val Login = "login"
		val LoginSuccess = "loginSuccess"
	    val JoinRoom = "joinRoom"
	    val LeaveRoom = "leaveRoom"
	    val RoomJoined = "roomJoined"
	    val RoomLeaved = "roomLeaved"
	    val ListRooms = "listRooms"
	    val RoomList = "roomList"
	    val CreateRoom = "createRoom"
	    val RoomCreated = "roomCreated"
	    val DestroyRoom = "destroyRoom"
	    val RoomDestroyed = "roomDestroyed"
	    val SetRoomConfig = "setRoomConfig"
	    val RoomConfigSet = "roomConfigSet"
	    val GetRoomConfig = "getRoomConfig"
	    val RoomConfigResult = "roomConfigResult"
	    val KickUser = "kickUser"
	    val BanUser = "banUser" //TODO: super ban user based on IP address??
	    val UnBanUser = "unBanUser"
	    val RoomBroadcast = "roomBroadcast"
	    val PrivateMessage = "privateMessage"
	    val Disconnect = "disconnect"
	    val Echo = "echo"
	    val ActionFailed = "actionFailed"
	    val NotImplemented = "notImplemented"
	    val Unknown = "unknown"
	}
}