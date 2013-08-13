package com.blacklander.messagebus.messageprocessors
import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.Constants.Messages._
import scala.actors.Actor
import com.blacklander.messagebus.RoomInfo
import com.blacklander.messagebus.ExternalMessage

class RoomSecurityProcessor(ctx: Actor, game: Actor, roomInfo: RoomInfo) extends MessageProcessor {
	
    var players = Map[String, Actor]()
    
    def processMessage(message: ExternalMessage): Boolean = {
	    val userName = message.userName.get
	    message.messageType match {
	        case RoomBroadcast => true
	        case SetRoomConfig => true
	        case GetRoomConfig => true
	        case JoinRoom => {
	            println("RoomSecurityProcessor: JoinRoom")
	            //TODO: better security here...
	            players += (userName -> message.sender)
	            //game needs to know the action succeeded as well as the player
	            val msg = ExternalMessage(
	                ctx,
	                Map(MessageType -> RoomJoined, UserName -> userName, RoomId -> message.roomId.get),
	                None
	            )
	            game ! msg
	            message.sender ! msg
	            true
	        }
	        case LeaveRoom => {
	            players -= userName
	            true
	        }
	        case Disconnect => {
	            players -= userName
	            true
	        }
	        case DestroyRoom => {
	            //TODO: better security here...
	            //game needs to know the action succeeded as well as the player
	            val msg = ExternalMessage(
	                ctx,
	                Map(MessageType -> RoomDestroyed, UserName -> userName, RoomId -> message.roomId.get),
	                None
	            )
	            game ! msg
	            //sender doesn't necessarily need to be in the room
	            (message.sender :: players.values.toList).toSet[Actor].foreach({
	                _ ! msg
	            })
	            true
	        }
	        case KickUser => true
	        case BanUser => true
	        case UnBanUser => true
	        //TODO: handle admin, disconnect and config
	    }
	}
}