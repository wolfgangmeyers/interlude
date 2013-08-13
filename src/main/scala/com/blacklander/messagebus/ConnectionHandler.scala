package com.blacklander.messagebus

import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers
import scala.actors.Actor
import Util._
import Constants._
import Constants.Messages._

class ConnectionHandler(channel: Channel, private var game: Game) extends Actor {
	var userName: Option[String] = None
	var connected = true
	val buffer: ChannelBuffer = ChannelBuffers.dynamicBuffer()
	
	def act() {
	    val sender = this
	    react {
	        case ReadSignal(messageBytes) => {
	            println("ConnectionHandler ReadSignal")
	            //inject userName if we have it...
	            val message = parseMessage(messageBytes) match {
	                case (headers, body) => {
	                    ExternalMessage(
	                        sender,
	                        userName match {
	                            case Some(name) => headers + (UserName -> name)
	                            case None => headers
	                        },
	                        body
	                    )
	                }
	            }
	            
                //println("ConnectionHandler: Handling message")
                game ! message
                message.messageType match {
                    //this represents an expected disconnect,
                    //because the client explicitly indicated a desire to close the connection
                    case Disconnect => {
                        connected = false
                        handleDisconnect()
                    }
                    case _ => //do nothing
                }
	            
	            if(connected){
	            	act()
	            }
	        }
	        case msg: ExternalMessage => {
	            println("ConnectionHandler writing message: " + msg.messageType)
	            //used in multiple cases
	            def handleWriteMessage(msg: ExternalMessage) {
	                if(connected){
	                    writeMessage(msg, channel, buffer)
	                    act()
                    }
	            }
	            msg.messageType match {
	                //some of these messages get fired back to the handler
	                //LoginSuccess lets us know that the userName of the login is valid
	                //Disconnect represents a potential unexpected Disconnect
	                //(connected == true will indicate that it wasn't expected,
	                //the client deliberately closed the connection)
	                //in this case, the connection handler will fire the same
	                //disconnect event with username added
	                case Disconnect => {
	                    (userName, connected) match {
	                        case (Some(name), true) => {
	                            connected = false
	                            game ! msg.copy(headers=msg.headers + (UserName -> name))
	                        }
	                        case _ => //no need to handle disconnect with no login...
	                    }
	                }
	                //TODO: figure out how to avoid code duplication here
	                case LoginSuccess => {
	                    userName = msg.userName
	                    handleWriteMessage(msg)
	                }
	                case Hello => {
	                    msg.sender ! ExternalMessage(
	                        this, Map(MessageType -> Hello), None
	                    )
	                }
	                case _ => {
	                    handleWriteMessage(msg)
	                }
	            }
	        }
	        case msg: ExitSignal => {
	           connected = false
	           println("ConnectionHandler ExitSignal")
	           handleDisconnect()
	        }
	    }
	}
	
	def handleDisconnect() {
	    if(channel.isOpen()){
    	    channel.close().awaitUninterruptibly()
        }
	}
}