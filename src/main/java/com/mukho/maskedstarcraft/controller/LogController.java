package com.mukho.maskedstarcraft.controller;

import com.mukho.maskedstarcraft.dto.response.ApiResponse;
import com.mukho.maskedstarcraft.dto.response.TournamentLogResponse;
import com.mukho.maskedstarcraft.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class LogController {
    
    private final LogService logService;
    
    @GetMapping("/tournaments")
    public ResponseEntity<ApiResponse<List<TournamentLogResponse>>> getAllTournamentLogs() {
        List<TournamentLogResponse> logs = logService.getAllTournamentLogs();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    @GetMapping("/tournaments/{tournamentId}")
    public ResponseEntity<ApiResponse<TournamentLogResponse>> getTournamentLog(@PathVariable Long tournamentId) {
        TournamentLogResponse log = logService.getTournamentLog(tournamentId);
        return ResponseEntity.ok(ApiResponse.success(log));
    }
    
    @GetMapping("/tournaments/{tournamentId}/download")
    public ResponseEntity<Resource> downloadTournamentLog(@PathVariable Long tournamentId) {
        return logService.downloadTournamentLog(tournamentId);
    }
}
