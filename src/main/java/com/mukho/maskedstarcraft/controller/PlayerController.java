package com.mukho.maskedstarcraft.controller;

import com.mukho.maskedstarcraft.dto.response.ApiResponse;
import com.mukho.maskedstarcraft.dto.response.PlayerResponse;
import com.mukho.maskedstarcraft.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Slf4j
public class PlayerController {
    
    private final PlayerService playerService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlayerResponse>>> getPlayers() {
        List<PlayerResponse> players = playerService.getPlayers();
        return ResponseEntity.ok(ApiResponse.success(players));
    }
    
    @DeleteMapping("/{playerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlayer(@PathVariable Long playerId) {
        playerService.deletePlayer(playerId);
        return ResponseEntity.ok(ApiResponse.success("플레이어가 삭제되었습니다.", null));
    }
    
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAllPlayers() {
        playerService.deleteAllPlayers();
        return ResponseEntity.ok(ApiResponse.success("모든 플레이어가 삭제되었습니다.", null));
    }
}
