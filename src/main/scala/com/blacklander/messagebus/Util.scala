package com.blacklander.messagebus
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import scala.annotation.tailrec
import Constants._
import Constants.Messages._
import scala.collection.mutable.ArrayBuffer
import java.io.IOException
import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffer

object Util {
    
    private def parseHeaders(data: Array[Byte]): Map[String, String] = {
	    //bytes to string
	    //split string into lines
	    //split each line by "="
	    new String(data, "UTF-8")
	    	.split("\n").map(_.split("=", 2))
	    	.filter(_.length == 2).map(item => (item(0), item(1))).toMap
	}
    
    //TODO: use List[Array[Byte]] instead, reverse, then reduce
//    @tailrec
//	def readBytes(sc: SocketChannel, buff: ByteBuffer, head: Array[Byte]=Array[Byte]()): Array[Byte] = {
//	    buff.clear()
//        val numRead = sc.read(buff)
//        numRead match {
//	        case -1 => throw new IOException("Disconnected")
//	        case 0 => head
//	        case _ => {
//	            val bytes = new Array[Byte](numRead)
//		        buff.flip()
//		        buff.get(bytes)
//		        readBytes(sc, buff, head ++ bytes)
//	        }
//	    }
//	}
    
//    def parseMessages(messageStreamBytes: Array[Byte]): List[(Map[String, String], Option[Array[Byte]])] = {
//        //val messageStreamBytes = readBytes(sc, buff)
//        //println("Util messageStreamBytes: " + messageStreamBytes.length)
//        parseMessageFrames(ByteBuffer.wrap(messageStreamBytes))
//	               .map(parseMessage(_))
//    }
    
//    @tailrec
//    private def parseMessageFrames(data: ByteBuffer, tail: List[Array[Byte]]=List()): List[Array[Byte]] = {
//        if(data.remaining() == 0) {
//            tail
//        }else{
//            val messageLength = data.getInt()
//            //println("Util messageLength: " + messageLength)
//            val messageBytes = new Array[Byte](messageLength)
//            data.get(messageBytes)
//            parseMessageFrames(data, messageBytes :: tail)
//        }
//    }
    
    def parseMessage(data: Array[Byte]): (Map[String, String], Option[Array[Byte]]) = {
        data.indexOf(0x00) match {
            case -1 => (parseHeaders(data), None)
            case i => {
                val (headerBytes, body) = data.splitAt(i)
                //NOTE: splitAt(int) does not remove the \u0000 character, so need to drop it
                (parseHeaders(headerBytes), Some(body.drop(1)))
            }
        }
    }
    
//    def writeMessage(message: ExternalMessage, sc: SocketChannel, buff: ByteBuffer) = {
//        val messageFrame = serializeExternalMessage(message)
//        writeMessageFrame(messageFrame, sc, buff)
//    }
    
    def writeMessage(message: ExternalMessage, channel: Channel, buff: ChannelBuffer) {
        val messageFrame = serializeExternalMessage(message)
        //buff.setBytes(0, messageFrame)
        buff.clear()
        buff.resetReaderIndex()
        buff.resetWriterIndex()
        buff.writeBytes(messageFrame)
        channel.write(buff).awaitUninterruptibly()
    }
    
//    @tailrec
//    private def writeMessageFrame(messageFrame: Array[Byte], sc:SocketChannel, buff: ByteBuffer) {
//        messageFrame match {
//            case Array() => //all data has been written
//            case _ => {
//                buff.clear()
//		        buff.put(messageFrame)
//		        val chunk = buff.position()
//		        buff.flip()
//		        sc.write(buff)
//		        writeMessageFrame(messageFrame.drop(chunk), sc, buff)
//            }
//        }
//    }
    
    private def serializeExternalMessage(message: ExternalMessage): Array[Byte] = {
        val headerBytes = message.headers.map(h => h._1 + "=" + h._2).mkString("\n").getBytes("UTF-8")
        val messageBytes = message.data match {
            case Some(data) => headerBytes ++ Array[Byte](0) ++ data
            case None => headerBytes
        }
        val messageFrame = new Array[Byte](messageBytes.length + 4)
        val messageBuffer = ByteBuffer.wrap(messageFrame)
        messageBuffer.putInt(messageBytes.length)
        messageBuffer.put(messageBytes)
        messageFrame
    }
}