package com.blacklander.messagebus
import java.nio.channels.SocketChannel
import java.nio.channels.SelectionKey
import scala.actors.Actor

import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.Constants.Messages._

abstract class Message

case class ReadSignal(messageBytes: Array[Byte]) extends Message
case class ExitSignal extends Message
case class GameAssigned(game: Actor) extends Message

case class ExternalMessage(sender: Actor, headers: Map[String, String], data: Option[Array[Byte]]) {
    def messageType = headers.getOrElse(MessageType, Unknown)
    def userName = headers.get(UserName)
    def roomId = headers.get(RoomId)
    def gameId = headers.get(GameId)
}
