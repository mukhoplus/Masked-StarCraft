package com.mukho.maskedstarcraft.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mukho.maskedstarcraft.dto.request.GameResultRequest;
import com.mukho.maskedstarcraft.dto.response.ApiResponse;
import com.mukho.maskedstarcraft.dto.response.TournamentResponse;
import com.mukho.maskedstarcraft.service.TournamentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class TournamentController {
    
    private final TournamentService tournamentService;
    
    @GetMapping("/tournaments/current")
    public ResponseEntity<ApiResponse<TournamentResponse>> getCurrentTournament() {
        TournamentResponse response = tournamentService.getCurrentTournament();
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success("진행 중인 대회가 없습니다.", null));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/tournaments/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TournamentResponse>> startTournament() {
        TournamentResponse response = tournamentService.startTournament();
        return ResponseEntity.ok(ApiResponse.success("대회가 시작되었습니다.", response));
    }
    
    @PostMapping("/games/result")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TournamentResponse>> recordGameResult(@Valid @RequestBody GameResultRequest request) {
        TournamentResponse response = tournamentService.recordGameResult(request);
        return ResponseEntity.ok(ApiResponse.success("경기 결과가 기록되었습니다.", response));
    }
    
    @PostMapping("/tournaments/refresh")
    public ResponseEntity<ApiResponse<TournamentResponse>> refreshTournament() {
        TournamentResponse response = tournamentService.getCurrentTournament();
        return ResponseEntity.ok(ApiResponse.success("대회 정보가 새로고침되었습니다.", response));
    }
}
