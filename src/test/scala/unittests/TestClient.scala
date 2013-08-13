package unittests
import scala.actors.Actor
import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.Constants.Messages._
import com.blacklander.messagebus.Util._
import java.nio.channels.SocketChannel
import java.nio.ByteBuffer
import java.net.InetSocketAddress
import com.blacklander.messagebus.ExternalMessage
import com.blacklander.messagebus.ExitSignal
import com.blacklander.messagebus.Util
import com.blacklander.messagebus.RoomInfo
import com.codahale.jerkson.Json._
import scala.actors.TIMEOUT
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.ExceptionEvent
import java.util.concurrent.Executors
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.Channels
import com.blacklander.messagebus.MessageFrameDecoder
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBuffers

class TestClient() extends SimpleChannelHandler with Actor {
    val host = "localhost"
    val port = 9090
    val bossExecutor = Executors.newCachedThreadPool()
    val workerExecutor = Executors.newCachedThreadPool()
    val channelFactory = new NioClientSocketChannelFactory(bossExecutor, workerExecutor)
    val bootstrap = new ClientBootstrap(channelFactory)
    class MessagePipelineFactory extends ChannelPipelineFactory {
        override def getPipeline(): ChannelPipeline = {
            return Channels.pipeline(
                new MessageFrameDecoder(),
                TestClient.this
            )
        }
    }
    bootstrap.setPipelineFactory(new MessagePipelineFactory())
    bootstrap.setOption("tcpNoDelay", true)
    bootstrap.setOption("keepAlive", true)
    val channel = bootstrap.connect(new InetSocketAddress(host, port)).awaitUninterruptibly().getChannel()
    val buff = ChannelBuffers.dynamicBuffer()
    
    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
        val buff: ChannelBuffer = e.getMessage().asInstanceOf[ChannelBuffer]
        val messageBytes = new Array[Byte](buff.readableBytes())
        buff.readBytes(messageBytes)
        val msg = parseMessage(messageBytes) match {
            case (headers, bytes) => {
                 ExternalMessage(
                     this, 
                     headers,
                     bytes
        		 )
             }
         }
            
         println("TestClient read message - " + msg.messageType)
         listener ! msg
    
    }
    
    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
        e.getCause().printStackTrace()
    }
    
    
    println("TestClient created")
    def messages = listener.messages
    
    private val listener: TestClientListener = new TestClientListener()
//	private val s = SocketChannel.open()
//    s.connect(new InetSocketAddress("localhost", 9090))
//    s.configureBlocking(false)
//    val buff = ByteBuffer.allocateDirect(4096)
//    var running = false
    
    /* UTILITIES */
    def clearMessages() = {
        listener.messages = List()
    }
    
    def isAuthenticated: Boolean = {
        hasMessage(LoginSuccess)
    }
    
    def hasMessage(messageType: String): Boolean = {
        hasMessages(messageType, 1)
    }
    
    def hasMessages(messageType: String, count: Int): Boolean = {
        messages.filter(_.messageType == messageType).length == count
    }
    
    def getMessage(messageType: String): ExternalMessage = {
        getMessages(messageType)(0)
    }
    
    def getMessages(messageType: String): List[ExternalMessage] = {
        messages.filter(_.messageType == messageType)
    }
    
    def shutdown() {
        this ! ExitSignal()
    }
    
    
    /* MESSAGES */
    def login(username: String, password: String) {
	    this ! ExternalMessage(this, Map(MessageType -> Login, UserName -> username, Password -> password), None)
	}
	
	def echo(data: String) {
	    this ! ExternalMessage(this, Map(MessageType -> Echo), Some(data.getBytes("UTF-8")))
	}
	
	def privateMessage(recipient: String, message: String) {
	    val msg = ExternalMessage(
	        null,
	        Map(Recipient -> recipient, MessageType -> PrivateMessage),
	        Some(message.getBytes("UTF-8"))
	    )
	    this ! msg
	}
	
	def joinRoom(roomId: String) {
	    val msg = ExternalMessage(
	        null,
	        Map(MessageType -> JoinRoom, RoomId -> roomId),
	        None
	    )
	    this ! msg
	}
	
	def createRoom(roomInfo: RoomInfo) {
	    val msg = ExternalMessage(
	        null,
	        Map(MessageType -> CreateRoom),
	        Some(generate(roomInfo).getBytes("UTF-8"))
	    )
	    this ! msg
	}
	
	def destroyRoom(roomId: String) {
	    val msg = ExternalMessage(
	        null,
	        Map(MessageType -> DestroyRoom, RoomId -> roomId),
	        None
	    )
	    this ! msg
	}
	
	def listRooms() {
	    val msg = ExternalMessage(
	        null,
	        Map(MessageType -> ListRooms),
	        None
	    )
	    this ! msg
	}
	
	def roomBroadcast(roomId: String, message: String) {
	    val msg = ExternalMessage(
	        null,
	        Map(MessageType -> RoomBroadcast, RoomId -> roomId),
	        Some(message.getBytes("UTF-8"))
	    )
	    this ! msg
	}
    
    def act() {
        Thread.sleep(100)
        listener.start()
        //println("client starting up")
//        running = true
        //buff.clear()
        //val headers = Map(UserName -> username, MessageType -> Login)
        //ExternalMessage(sender: Actor, messageType: String, headers: Map[String, String], data: Option[Array[Byte]])
	    //writeMessage(ExternalMessage(null, headers, None), s, buff)
        
       // while(running){
        react {
            case msg: ExitSignal => {
                //println("client shutting down")
//                val headers = Map(MessageType -> Disconnect)
//                writeMessage(ExternalMessage(null, headers, None), channel, buff)
//                Thread.sleep(200)
                channel.close().awaitUninterruptibly()
                bossExecutor.shutdown()
                workerExecutor.shutdown()
                //relay exit signal to listener, it should shut down as well
                listener ! msg
            }
            case msg: ExternalMessage => {
                println("TestClient: writing message to server: " + msg.messageType)
                writeMessage(msg, channel, buff)
                //writeMessage(msg, s, buff)
                act()
            }
        }
    }
}
