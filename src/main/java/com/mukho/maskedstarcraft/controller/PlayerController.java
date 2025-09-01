package com.mukho.maskedstarcraft.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mukho.maskedstarcraft.dto.request.ApplyRequest;
import com.mukho.maskedstarcraft.dto.response.ApiResponse;
import com.mukho.maskedstarcraft.dto.response.PlayerResponse;
import com.mukho.maskedstarcraft.service.AuthService;
import com.mukho.maskedstarcraft.service.PlayerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
@Slf4j
public class PlayerController {
    
    private final PlayerService playerService;
    private final AuthService authService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> applyPlayer(@RequestBody ApplyRequest request) {
        authService.apply(request);
        return ResponseEntity.ok(ApiResponse.success("참가 신청이 완료되었습니다.", null));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlayerResponse>>> getPlayers() {
        List<PlayerResponse> players = playerService.getPlayers();
        return ResponseEntity.ok(ApiResponse.success(players));
    }
    
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> cancelMyParticipation() {
        playerService.cancelMyParticipation();
        return ResponseEntity.ok(ApiResponse.success("참가가 취소되었습니다.", null));
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
