package pdl.backend;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    @MessageMapping("/flip")
    @SendTo("/topic/flipped")
    public FlipMessage flipCard(FlipMessage message) {
        // Here you could add server validation if needed
        return message;
    }

    @MessageMapping("/restart")
    @SendTo("/topic/restart")
    public String restartGame(String msg) {
        return "restart";
    }

    @MessageMapping("/getusers")
    @SendTo("/topic/getusers")
    public String getUsers() {
        return "users";
    }

    @MessageMapping("/match")
    @SendTo("/topic/match")
    public MatchRequest matchCards(MatchRequest request) {
        return request;
    }

    @MessageMapping("/gethost")
    @SendTo("/topic/gethost")
    public String getHost() {
        return "host";
    }



}

