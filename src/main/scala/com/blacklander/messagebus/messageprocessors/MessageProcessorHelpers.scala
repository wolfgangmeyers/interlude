package com.blacklander.messagebus.messageprocessors
import com.blacklander.messagebus.ExternalMessage

//TODO: investigate the right way to do this...
trait MessageProcessorHelpers {
	def UserRequired(message: ExternalMessage)(f: ExternalMessage => Boolean) = {
	    message.userName match {
	        case Some(userName) => f(message)
	        case None => {
	            true
	        }
	    }
	}
}