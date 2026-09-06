package pdl.backend;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionTracker {

    private final Map<String, PlayerInfo> connectedSessions = new ConcurrentHashMap<>();
    private String host = "";


    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        connectedSessions.remove(sessionId);
        if (host.equals(sessionId) && !connectedSessions.isEmpty()) {
            host = connectedSessions.keySet().iterator().next();
        }
        System.out.println("Disconnected: " + sessionId);
    }

    public Map<String, PlayerInfo> getConnectedSessions() {
        return connectedSessions;
    }

    public void addSession(String sessionId, PlayerInfo player) {
        connectedSessions.put(sessionId, player);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void updateScore(String sessionId, int score) {
        PlayerInfo player = connectedSessions.get(sessionId);
        player.setScore(score);
        connectedSessions.put(sessionId, player);
    }


}
