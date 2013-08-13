package com.blacklander.messagebus

import org.jboss.netty.handler.codec.replay.{ReplayingDecoder,VoidEnum}
import org.jboss.netty.channel._
import org.jboss.netty.buffer._

class MessageFrameDecoder extends ReplayingDecoder[VoidEnum]{
	override def decode(ctx: ChannelHandlerContext, channel: Channel, buff: ChannelBuffer, state: VoidEnum): Object = {
	    return buff.readBytes(buff.readInt())
	}
}