package com.blacklander.messagebus.messageprocessors

import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.Constants.Messages._
import scala.actors.Actor

class RoomBroadcastProcessor(ctx: Actor) extends MessageProcessor {
    
    var players = Map[String, Actor]()
    
	def processMessage(message: ExternalMessage): Boolean = {
        val userName = message.userName.get
	    message.messageType match {
	        case JoinRoom => {
	            players += (userName -> message.sender)
	        }
	        case LeaveRoom => {
	            players -= userName
	        }
	        //TODO: avoid code duplication here
	        case RoomBroadcast => {
	            players foreach { kv =>
	                if(kv._1 != userName) kv._2 ! message
	            }
	        }
	        case Hello => {
	            players foreach { kv =>
	                if(kv._1 != userName) kv._2 ! message
	            }
	        }
	    }
	    true
	}
}