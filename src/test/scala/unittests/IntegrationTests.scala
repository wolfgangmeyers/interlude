package unittests
import org.scalatest.FunSuite
import com.blacklander.messagebus.ExitSignal
import com.blacklander.messagebus.Constants._
import com.blacklander.messagebus.Constants.Messages._
import com.blacklander.messagebus.Game
import com.blacklander.messagebus.messageprocessors._
import com.blacklander.messagebus.RoomInfo
import com.blacklander.messagebus.Room
import com.blacklander.messagebus.ExitSignal
import org.scalatest.BeforeAndAfterAll
import com.blacklander.messagebus.ExternalMessage
import com.codahale.jerkson.Json._
import org.scalatest.Assertions
import com.blacklander.messagebus.MessageServer

class IntegrationTests extends FunSuite with TestUtils with BeforeAndAfterAll{
    
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
            RoomDestroyed, SetRoomConfig, GetRoomConfig, KickUser, BanUser, UnBanUser, Hello)
//    val listener = new LegacyConnectionListener(9090, defaultGame)
      val server = new MessageServer("localhost", 9090, defaultGame)
           
    override def beforeAll() {
        defaultGame.start()
        Thread.sleep(1000)
        println("integration test server started")
    }
    
    override def afterAll() {
        defaultGame ! ExitSignal()
        server.shutdown()
        Thread.sleep(100)
        println("integration test server stopped")
    }
    
	test("Login succeeds and Echo message returns") {
	    val tc = new TestClient()
	    tc.start()
	    try{
	        tc.login("foo", "bar")
	        waitUntil(1000, "Login timeout") {tc.isAuthenticated}
		    tc.echo("Hello there!")
		    waitUntil(1000, "Echo timeout") {tc.hasMessage(Echo)}
		    val msg = tc.getMessage(Echo)
		    assert(msg.data match {
		        case Some(bytes) => {
		            new String(bytes, "UTF-8") == "Hello there!"
		        }
		        case None => false
		    })
	    } finally {
	        tc.shutdown()
	    }
	}
	
	test("Login succeeds and client receives message from another client") {
	    val client1 = new TestClient()
	    val client2 = new TestClient()
	    client1.start()
	    client2.start()
	    try{
	        client1.login("client1", "client1")
		    client2.login("client2", "client2")
		    waitUntil(1000, "Login timeout") {client1.isAuthenticated && client2.isAuthenticated}
		    client1.privateMessage("client2", "Hello, there!")
		    waitUntil(1000, "PrivateMessage timeout") {client2.hasMessage(PrivateMessage)}
		    assert(!client1.hasMessage(PrivateMessage))
		    assert(client2.hasMessages(PrivateMessage, 1))
		    val privMsg = client2.getMessage(PrivateMessage)
		    assert(privMsg.userName === Some("client1"))
		    assert(privMsg.headers.get(Recipient) === Some("client2"))
		    assert(privMsg.data match {
		        case Some(bytes) => {
		            new String(bytes, "UTF-8") == "Hello, there!"
		        }
		        case None => false
		    })
	    } finally {
	        client1.shutdown()
	        client2.shutdown()
	    }
	}
	
	test("join non-existent room returns RoomDestroyed message") {
	    val client = new TestClient()
	    client.start()
	    try {
	        client.login("client", "password")
	        waitUntil(1000, "Login timeout") {client.isAuthenticated}
	        client.joinRoom("nonexistent")
	        waitUntil(1000, "JoinRoom timeout") {client.hasMessage(RoomDestroyed)}
	    } finally {
	        client.shutdown()
	    }
	}
	
	test("create, join, and destroy room") {
	    val client = new TestClient()
	    client.start()
	    try {
	        client.login("client", "password")
	        waitUntil(1000, "Login timeout") {client.isAuthenticated}
	        val roomInfo = RoomInfo(
	            "testRoom1",
	            "client",
	            false,
	            true
	        )
	        client.createRoom(roomInfo)
	        waitUntil(1000, "CreateRoom timeout") {client.hasMessage(RoomCreated)}
	        val roomCreatedMsg = client.getMessage(RoomCreated)
	        assert(roomCreatedMsg.data match {
	            case Some(bytes) => {
	                parse[RoomInfo](new String(bytes, "UTF-8")) == roomInfo
	            }
	            case None => false
	        })
	        client.joinRoom("testRoom1")
	        waitUntil(1000, "JoinRoom timeout") {client.hasMessage(RoomJoined)}
	        val roomJoinedMsg = client.getMessage(RoomJoined)
	        assert(roomJoinedMsg.roomId === Some("testRoom1"))
	        client.destroyRoom("testRoom1")
	        waitUntil(1000, "DestroyRoom timeout") {client.hasMessage(RoomDestroyed)}
	        val roomDestroyedMsg = client.getMessage(RoomDestroyed)
	        assert(roomDestroyedMsg.roomId === Some("testRoom1"))
	    } finally {
	        client.shutdown()
	    }
	}
	
	test("Create visible and hidden rooms, room list excludes hidden, destroyRoom removes rooms from list") {
	    val client1 = new TestClient()
	    val client2 = new TestClient()
	    Thread.sleep(1000)
	    client1.start()
	    client2.start()
	    try {
	        client1.login("client1", "client1")
	        client2.login("client2", "client2")
	        waitUntil(1000, "Login timeout") {client1.isAuthenticated && client2.isAuthenticated}
	        val roomTwo = RoomInfo(
	            "testRoom2",
	            "client2",
	            false,
	            true
	        )
	        val roomThree = RoomInfo(
	            "testRoom3",
	            "client2",
	            false,
	            true
	        )
	        val roomFour = RoomInfo(
	            "testRoom4",
	            "client2",
	            false,
	            false
	        )
	        client1.createRoom(roomTwo)
	        client2.createRoom(roomThree)
	        client2.createRoom(roomFour)
	        waitUntil(1000, "CreateRoom timeout") {
	            client1.hasMessage(RoomCreated) && client2.hasMessages(RoomCreated, 2)
	        }
	        //only room2 and room3 should be visible
	        client1.listRooms()
	        client2.listRooms()
	        waitUntil(1000, "ListRooms timeout") {
	            client1.hasMessage(RoomList) && client2.hasMessage(RoomList)
	        }
	        val roomList1 = client1.getMessage(RoomList).data.map(new String(_, "UTF-8")).map(parse[List[RoomInfo]])
	        	.getOrElse(Assertions.fail("roomList1 has no data"))
	        val roomList2 = client2.getMessage(RoomList).data.map(new String(_, "UTF-8")).map(parse[List[RoomInfo]])
	        	.getOrElse(Assertions.fail("roomList2 has no data"))
	        assert(roomList1 === roomList2)
	        assert(roomList1.contains(roomTwo))
	        assert(roomList1.contains(roomThree))
	        assert(!roomList1.contains(roomFour))
	        
	        //clean up
	        client1.destroyRoom("testRoom2")
	        client2.destroyRoom("testRoom3")
	        client2.destroyRoom("testRoom4")
	        waitUntil(1000, "DestroyRoom timeout") {
	            client1.hasMessage(RoomDestroyed) && client2.hasMessages(RoomDestroyed, 2)
	        }
	        //make sure the rooms are really gone
	        client1.clearMessages()
	        client1.listRooms()
	        waitUntil(1000, "ListRooms timeout") {
	            client1.hasMessage(RoomList)
	        }
	        val roomList3 = client1.getMessage(RoomList).data.map(new String(_, "UTF-8")).map(parse[List[RoomInfo]])
	            .getOrElse(Assertions.fail("roomList3 has no data"))
	        assert(roomList3 === Nil)
	        //TODO: implement
	    } finally {
	        client1.shutdown()
	        client2.shutdown()
	    }
	}
	
	test("Join invisible room, RoomBroadcast reaches all users in a room, no users out of room") {
	    val client1 = new TestClient()
	    val client2 = new TestClient()
	    val client3 = new TestClient()
	    val client4 = new TestClient()
	    client1.start()
	    client2.start()
	    client3.start()
	    client4.start()
	    try {
	        client1.login("client1", "client1")
	        client2.login("client2", "client2")
	        client3.login("client3", "client3")
	        client4.login("client4", "client4")
	        waitUntil(1000, "Login timeout") {
	            (client1.isAuthenticated && client2.isAuthenticated
	              && client3.isAuthenticated && client4.isAuthenticated)
	        }
	        val roomFive = RoomInfo(
	            "testRoom5",
	            "client1",
	            false,
	            false
	        )
	        val roomSix = RoomInfo(
	            "testRoom6",
	            "client4",
	            false,
	            false
	        )
	        client1.createRoom(roomFive)
	        client4.createRoom(roomSix)
	        waitUntil(1000, "CreateRoom timeout") {
	            client1.hasMessage(RoomCreated) && client4.hasMessage(RoomCreated)
	        }
	        client1.joinRoom("testRoom5")
	        client2.joinRoom("testRoom5")
	        client3.joinRoom("testRoom5")
	        client4.joinRoom("testRoom6")
	        waitUntil(1000, "JoinRoom timeout") {
	            (client1.hasMessage(RoomJoined)
	                    && client2.hasMessage(RoomJoined)
	                    && client3.hasMessage(RoomJoined)
	                    && client4.hasMessage(RoomJoined))
	        }
	        client2.roomBroadcast("testRoom5", "Hello from client2!!!")
	        waitUntil(1000, "RoomBroadcast timeout") {
	            (client1.hasMessage(RoomBroadcast)
	                    && client3.hasMessage(RoomBroadcast))
	        }
	        //wait a bit longer in case the other two "catch up"
	        Thread.sleep(100)
	        assert(!client2.hasMessage(RoomBroadcast))
	        assert(!client4.hasMessage(RoomBroadcast))
	        client2.destroyRoom("testRoom5")
	        client4.destroyRoom("testRoom6")
	        //client1 and client3 should be notified of room destruction
	        //since they are in the room
	        waitUntil(1000, "DestroyRoom timeout") {
	            (client2.hasMessage(RoomDestroyed) 
	                    && client4.hasMessage(RoomDestroyed)
	                    && client1.hasMessage(RoomDestroyed)
	                    && client3.hasMessage(RoomDestroyed))
	        }
	    } finally {
	        client1.shutdown()
	        client2.shutdown()
	    }
	}
}