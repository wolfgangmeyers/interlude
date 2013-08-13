package test

import com.codahale.jerkson.Json._
import com.blacklander.messagebus.RoomInfo

object TestJson extends App {
    case class Person(name: String, age: Int)
    println(parse[Person]("""{"name": "foobar", "age": 16}"""))
    val roomInfo = parse[RoomInfo]("""{
        "gameId": "foobar",
        "roomId": "fooroom",
        "createdBy": "foo",
        "inviteOnly": false,
        "isVisible": true
    }""")
    println(roomInfo)
    println(generate(roomInfo))
}