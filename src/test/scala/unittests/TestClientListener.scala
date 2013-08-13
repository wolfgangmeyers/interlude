package unittests
import scala.actors.Actor
import com.blacklander.messagebus.ExitSignal
import com.blacklander.messagebus.ExternalMessage

class TestClientListener extends Actor {
        
    var messages = List[ExternalMessage]()
    
    def act() {
        var running = true;
        while(running) {
            receive {
                case msg: ExternalMessage => {
                    messages = msg :: messages
                }
                case ExitSignal => running = false
            }
        }
    }
}