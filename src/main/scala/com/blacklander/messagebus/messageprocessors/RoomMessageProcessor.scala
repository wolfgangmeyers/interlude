package com.blacklander.messagebus.messageprocessors
import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.Room
import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.Constants.Messages._
import scala.actors.Actor
import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.ExternalMessage
import com.codahale.jerkson.Json._
import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.RoomInfo
import com.blacklander.messagebus.ExitSignal

class RoomMessageProcessor(ctx: Actor, roomFactory: RoomInfo => Room) extends MessageProcessor {
    
    var rooms: Map[String, Actor] = Map()
    var usersToRooms: Map[String, Actor] = Map()
    
	def processMessage(message: ExternalMessage): Boolean = {
        message.userName match {
            case Some(userName) => {
                message.messageType match {
			        case RoomBroadcast => routeToRoom(message)
			        case ListRooms => listRooms(message)
			        case CreateRoom => createRoom(message)
			        case RoomDestroyed => roomDestroyed(message)
			        case Disconnect => disconnect(message)
			        case RoomJoined => roomJoined(message)
			        case _ => routeToRoom(message)
			    }
            }
            case _ => println("Warning: RoomMessageProcessor called (" + message.messageType + ") with no userName")
        }
	    
	    true
	}
    
    override def onExit() {
        rooms foreach { kv =>
            kv._2 ! ExitSignal()
        }
    }
    
    def routeToRoom(message: ExternalMessage) {
        println("routeToRoom: " + message)
        message.roomId match {
            case Some(roomId) => rooms.get(roomId) match {
                case Some(room) => room ! message
                case None => message.sender ! ExternalMessage(ctx, 
                        Map(MessageType -> RoomDestroyed), None)
            }
            case None => println("Warning: Trying to route to room without room id")
        }
    }
    
    def notImplemented(message: ExternalMessage) {
        message.sender ! ExternalMessage(ctx, 
                Map(MessageType -> NotImplemented, Message -> (message.messageType + " is not implemented")),
                None)
    }
    
    def disconnect(message: ExternalMessage) {
		val userName = message.userName.get
        usersToRooms.get(userName) match {
            case Some(room) => {
                usersToRooms -= userName
                room ! message
            }
            case None => //do nothing
        }
    }
    
    def roomJoined(message: ExternalMessage) {
        val userName = message.userName.get
        message.roomId match {
            case Some(roomId) => {
                rooms.get(roomId) match {
                    case Some(room) => usersToRooms += (userName -> room)
                    case None => println("Warning: roomJoined message with invalid roomId")
                }
            }
            case None => println("Warning: roomJoined message with no roomId")
        }
        
    }
    
    def roomDestroyed(message: ExternalMessage) {
        message.roomId match {
            case Some(roomId) => rooms -= roomId
            case _ => println("Warning: roomDestroyed with no roomId")
        }
    }
    
    def listRooms(message: ExternalMessage) {
        val data = generate(
            rooms.values map (_.asInstanceOf[Room].roomInfo) filter (_.isVisible)
        )
        val headers = Map(MessageType -> RoomList)
        message.sender ! ExternalMessage(ctx, headers, Some(data.getBytes("UTF-8")))
    }
    
    def createRoom(message: ExternalMessage) {
        val result = message.data match {
            case Some(bytes) => {
                try {
                    val roomInfo = parse[RoomInfo](new String(bytes, "UTF-8"))
                    rooms.get(roomInfo.roomId) match {
                        case Some(room) => ExternalMessage(
                            ctx, Map(MessageType -> ActionFailed, Message -> "A room already exists with that name. Please try another name"), None
                        )
                        case None => {
                            val room = roomFactory(roomInfo)
                            rooms += (roomInfo.roomId -> room)
                            room.start()
                            //echo back the room info with roomCreated response
                            ExternalMessage(
                                ctx, Map(MessageType -> RoomCreated), Some(bytes)
                            )
                        }
                    }
                } catch {
                    case ex: Exception => ExternalMessage(
                        ctx, Map(MessageType -> ActionFailed, Message -> ("An error occurred while creating the room: " + ex.getMessage())), None
                    )
                }
            }
            case None => ExternalMessage(
                ctx, Map(MessageType -> ActionFailed, Message -> "Invalid message: body is required"), None
            )
        }
        message.sender ! result
    }
}