package pdl.backend;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/ws")
public class WebSocketSessionController {

    private final WebSocketSessionTracker tracker;

    public WebSocketSessionController(WebSocketSessionTracker tracker) {
        this.tracker = tracker;
    }

    @GetMapping("/users")
    public Map<String, PlayerInfo> getUsers() {
        return tracker.getConnectedSessions();
    }

    @GetMapping("/gethost")
    public String getHost() {
        return tracker.getHost();
    }

    @MessageMapping("/sethost")
    public void setHost(StompHeaderAccessor accessor) {
        String host = accessor.getSessionId();
        tracker.setHost(host);
    }
    
    

    @MessageMapping("/register")
    public void register(PlayerInfo info, StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        tracker.addSession(sessionId, info);

    }

    @MessageMapping("/updateScores")
    @SendTo("/topic/getusers")
    public String updateScores(int score, StompHeaderAccessor accessor) {
        tracker.updateScore(accessor.getSessionId(), score);
        return "users";
    }

    @MessageMapping("/generateGrid")
    @SendTo("/topic/grid")
    public List<MemoryCard> receiveGrid(@Payload List<MemoryCard> clientGrid) {
        return clientGrid;
    }


}
