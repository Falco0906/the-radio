package com.theradio.presence;

import com.theradio.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPresenceListener.class);

    private final ActiveUserRegistry activeUserRegistry;
    private final PresencePollingService pollingService;

    public WebSocketPresenceListener(ActiveUserRegistry activeUserRegistry,
                                      PresencePollingService pollingService) {
        this.activeUserRegistry = activeUserRegistry;
        this.pollingService = pollingService;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Long userId = extractUserId(accessor);
        if (userId != null) {
            activeUserRegistry.addUser(userId);
            log.info("WebSocket connected — tracking userId {}", userId);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Long userId = extractUserId(accessor);
        if (userId != null) {
            activeUserRegistry.removeUser(userId);
            pollingService.removeUserState(userId);
            log.info("WebSocket disconnected — untracking userId {}", userId);
        }
    }

    private Long extractUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            return null;
        }

        try {
            if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth) {
                Object principal = auth.getPrincipal();
                if (principal instanceof UserPrincipal userPrincipal) {
                    return userPrincipal.getId();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract userId from WebSocket session: {}", e.getMessage());
        }

        return null;
    }
}
