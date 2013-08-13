package com.blacklander.messagebus.messageprocessors
import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.Game

trait MessageProcessor {
	def processMessage(message: ExternalMessage): Boolean
	//can be overridden to handle server shutdown
	def onExit() {}
}