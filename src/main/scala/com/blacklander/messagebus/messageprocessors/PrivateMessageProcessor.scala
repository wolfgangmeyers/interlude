package com.blacklander.messagebus.messageprocessors
import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.Constants.Messages._
import scala.actors.Actor

class PrivateMessageProcessor extends MessageProcessor {
    
    var users = Map[String, Actor]()
    
	def processMessage(message: ExternalMessage): Boolean = {
        
        message.headers.get(UserName) match {
            case Some(username) => {
                 message.messageType match {
			        case Login => {
			            users += (username -> message.sender)
			        }
			        case Disconnect => {
			            users -= username
			        }
			        case PrivateMessage => {
			            message.headers.get(Recipient) match {
			                case Some(recipient) => {
			                    users.get(recipient) match {
			                        case Some(actor) => actor ! message
			                        case None => throw new IllegalArgumentException("PrivateMessage: Could not find recipient")
			                    }
			                }
			                case None => throw new IllegalArgumentException("PrivateMessage: No recipient was specified")
			            }
			        }
			        case _ => throw new IllegalArgumentException("Cannot handle message: " + message.messageType)
                 }
            }
            case _ => //do nothing
        }
	   
	    true
	}
}