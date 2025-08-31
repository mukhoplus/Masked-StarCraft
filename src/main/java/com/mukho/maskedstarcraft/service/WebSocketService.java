package com.mukho.maskedstarcraft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void broadcastTournamentUpdate() {
        try {
            messagingTemplate.convertAndSend("/topic/tournament", "update");
            log.info("Tournament update broadcasted");
        } catch (Exception e) {
            log.error("Failed to broadcast tournament update", e);
        }
    }
    
    public void broadcastGameResult(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/game-result", message);
            log.info("Game result broadcasted: {}", message);
        } catch (Exception e) {
            log.error("Failed to broadcast game result", e);
        }
    }
}
