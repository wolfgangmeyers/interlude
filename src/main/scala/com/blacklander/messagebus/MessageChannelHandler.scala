package com.blacklander.messagebus
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.ChannelStateEvent
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.group.ChannelGroup

class MessageChannelHandler(defaultGame: Game, clientChannels: ChannelGroup) extends SimpleChannelHandler{

    var connectionHandlers: Map[Integer, ConnectionHandler] = Map()
    
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
        //println("MessageChannelHandler: messageReceived")
        connectionHandlers.get(e.getChannel().getId()) match {
            case Some(connectionHandler) => {
                val buff: ChannelBuffer = e.getMessage().asInstanceOf[ChannelBuffer]
                val messageBytes = new Array[Byte](buff.readableBytes())
                buff.readBytes(messageBytes)
                connectionHandler ! ReadSignal(messageBytes)
            }
            case None => println("Warning: messageReceived, but no channel found with that id")
        }
    }
    
    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
        e.getCause().printStackTrace()
    }
    
    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
        //println("MessageChannelHandler: client connected")
        val id = e.getChannel().getId()
        val connectionHandler = new ConnectionHandler(e.getChannel, defaultGame)
        connectionHandlers += (id -> connectionHandler)
        clientChannels.add(e.getChannel())
        connectionHandler.start()
    }
    
    override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
        //println("MessageChannelHandler: client disconnected")
        connectionHandlers.get(e.getChannel().getId()) match {
            case Some(connectionHandler) => {
                connectionHandler ! ExitSignal()
                connectionHandlers -= e.getChannel().getId()
            }
            case None => println("Warning: channelDisconnect, but no channel found with that id")//ghost disconnect...?
        }
        clientChannels.remove(e.getChannel())
    }
    
    def shutdown() {
        connectionHandlers.values foreach {
            _.handleDisconnect() //blocks until connection is closed
        }
    }
}