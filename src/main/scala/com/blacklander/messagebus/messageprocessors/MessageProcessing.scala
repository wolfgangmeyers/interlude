package com.blacklander.messagebus.messageprocessors
import com.blacklander.messagebus.ExternalMessage
import scala.annotation.tailrec

trait MessageProcessing {
    
	var messageProcessors = Map[String, List[MessageProcessor]]()
	
	def registerMessageProcessor(processor: MessageProcessor, messageTypes: String*) {
        messageTypes foreach { messageType =>
	        val registered = messageProcessors.getOrElse(messageType, List[MessageProcessor]())
	    	messageProcessors += (messageType -> (processor :: registered))
        }
    }
	
	@tailrec
    protected[this] final def processMessages(processors: List[MessageProcessor], message: ExternalMessage) {
        
        processors match {
            case List() => //do nothing
            case processor :: moreProcessors => {
                //println("processMessage: " + message.messageType)
                //return false will end the loop
                if(processor.processMessage(message)){
                    processMessages(moreProcessors, message)
                }
            }
        }
    }
	
	protected[this] def onExit() {
	    //not very functional, but it gets the job done...
        var notified = Set[MessageProcessor]()
        messageProcessors foreach { kv =>
            kv._2 foreach { processor =>
                if(!notified.contains(processor)){
                    notified += processor
                    processor.onExit()
                }
            }
        }
	}
}