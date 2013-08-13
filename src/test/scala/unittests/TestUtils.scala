package unittests
import org.scalatest.Assertions
import com.blacklander.messagebus.ExternalMessage

trait TestUtils {
    
	def waitUntil(timeout: Int, message: String)(f: => Boolean) {
	    var t = 0
	    var success = false
	    while(t < timeout && !success) {
	        Thread.sleep(10)
	        t += 10
	        success = success || f
	    }
	    if(!success){
	        Assertions.fail(message)
	    }
	}
}