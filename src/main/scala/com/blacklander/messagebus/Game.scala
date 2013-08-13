package com.blacklander.messagebus
import scala.actors.Actor
import com.blacklander.messagebus.messageprocessors.MessageProcessor
import scala.annotation.tailrec
import com.blacklander.messagebus.Constants.Messages._
import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.messageprocessors.MessageProcessing

class Game extends Actor with MessageProcessing {
    
	def act() {
	    val sender = this
	    react {
	        case msg: ExternalMessage => {
	            println("Game: ExternalMessage: " + msg.messageType)
	            //TODO: return some error response for unrecognized messages?
	            messageProcessors.get(msg.messageType) match {
	                case Some(processors) => {
	                    try{
	                        processMessages(processors, msg)
	                    } catch {
	                        case ex: Exception => {
	                            ex.printStackTrace()
	                            //ExternalMessage(sender: Actor, messageType: String,
	                            //headers: Map[String, String], data: Option[Array[Byte]])
	                            val failedMessage = ExternalMessage(sender, Map(Message -> ex.getMessage(), MessageType -> ActionFailed), None)
	                            msg.sender ! failedMessage
	                        }
	                    }
	                }
	                case _ => println("Warning (game): unhandled (messageType=" + msg.messageType + ")") //drop message...
	            }
	            act()
	        }
	        case msg: ExitSignal => {
	            onExit()
	        }
	    }
	}
}