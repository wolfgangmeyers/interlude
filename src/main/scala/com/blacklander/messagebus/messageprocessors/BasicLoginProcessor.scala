package com.blacklander.messagebus.messageprocessors
import com.blacklander.messagebus.ExternalMessage
import scala.actors.Actor
import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.Constants.Messages._

class BasicLoginProcessor(ctx: Actor) extends MessageProcessor{
    
	def processMessage(message: ExternalMessage): Boolean = {
	    message.messageType match {
	        case Login => {
	            message.userName match {
	                //in a real login implementation, the
	                //password would be checked against a single sign-on setup
	                case Some(userName) => {
	                    println("BasicLoginProcessor: login succeeded - " + userName)
	                    message.sender ! ExternalMessage(ctx, Map(MessageType -> LoginSuccess, UserName -> userName), None)
	                    true
	                }
	                case None => false
	            }
	        }
	    }
	}
}