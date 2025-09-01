package com.mukho.maskedstarcraft.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    
    public void broadcastTournamentStart() {
        try {
            messagingTemplate.convertAndSend("/topic/tournament", "tournament_started");
            log.info("Tournament start broadcasted");
        } catch (Exception e) {
            log.error("Failed to broadcast tournament start", e);
        }
    }
    
    public void broadcastTournamentFinish(String winner) {
        try {
            messagingTemplate.convertAndSend("/topic/tournament", "tournament_finished:" + winner);
            log.info("Tournament finish broadcasted with winner: {}", winner);
        } catch (Exception e) {
            log.error("Failed to broadcast tournament finish", e);
        }
    }
    
    public void broadcastRefreshRequired() {
        try {
            messagingTemplate.convertAndSend("/topic/refresh", "refresh_required");
            log.info("Refresh required broadcasted");
        } catch (Exception e) {
            log.error("Failed to broadcast refresh requirement", e);
        }
    }
}
