
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.net.InetSocketAddress
import com.blacklander.messagebus.Game
import com.blacklander.messagebus.Constants.Messages._
import com.blacklander.messagebus.messageprocessors.EchoProcessor
import com.blacklander.messagebus.messageprocessors.PrivateMessageProcessor
import com.blacklander.messagebus.messageprocessors.BasicLoginProcessor
import com.blacklander.messagebus.messageprocessors.RoomMessageProcessor
import com.blacklander.messagebus.messageprocessors.MessageProcessor
import com.blacklander.messagebus.RoomInfo
import com.blacklander.messagebus.Room
import com.blacklander.messagebus.messageprocessors.RoomBroadcastProcessor
import com.blacklander.messagebus.messageprocessors.RoomSecurityProcessor
import com.blacklander.messagebus.MessageServer

object Interlude extends App {
    
    val parsedArgs = args.filter(_.contains("="))
    		.map(_.split("=")).map(a => (a(0), a(1))).toMap
    val port = parsedArgs.getOrElse("port", "9090").toInt
    
    val defaultGame = new Game()
    defaultGame.registerMessageProcessor(new EchoProcessor(), Echo)
    val privateMessageProcessor = new PrivateMessageProcessor()
    defaultGame.registerMessageProcessor(privateMessageProcessor, Login, Disconnect, PrivateMessage)
    defaultGame.registerMessageProcessor(new BasicLoginProcessor(defaultGame), Login)
    defaultGame.registerMessageProcessor(
            new RoomMessageProcessor(defaultGame, (roomInfo: RoomInfo) => {
                val room = new Room(roomInfo)
                room.registerMessageProcessor(new RoomBroadcastProcessor(room), JoinRoom, LeaveRoom, RoomBroadcast)
                room.registerMessageProcessor(
                    new RoomSecurityProcessor(room, defaultGame, roomInfo),
                    JoinRoom, LeaveRoom, Disconnect, DestroyRoom, KickUser, BanUser, UnBanUser, RoomBroadcast
                )
                room
            }),
            RoomBroadcast, JoinRoom, RoomJoined, LeaveRoom, ListRooms, CreateRoom, DestroyRoom, Disconnect,
            SetRoomConfig, GetRoomConfig, KickUser, BanUser, UnBanUser, Hello)
    
    defaultGame.start()
    val server = new MessageServer("localhost", 9090, defaultGame)
}