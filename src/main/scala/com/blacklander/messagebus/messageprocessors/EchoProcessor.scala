package com.blacklander.messagebus.messageprocessors
import com.blacklander.messagebus.Game
import com.blacklander.messagebus.ExternalMessage

class EchoProcessor extends MessageProcessor {
	def processMessage(message: ExternalMessage): Boolean = {
        println("EchoProcessor processMessage")
        message.sender ! message
        true
    }
}