package com.mukho.maskedstarcraft.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mukho.maskedstarcraft.dto.response.PlayerResponse;
import com.mukho.maskedstarcraft.entity.Tournament;
import com.mukho.maskedstarcraft.entity.User;
import com.mukho.maskedstarcraft.exception.BusinessException;
import com.mukho.maskedstarcraft.repository.TournamentRepository;
import com.mukho.maskedstarcraft.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlayerService {
    
    private final UserRepository userRepository;
    private final TournamentRepository tournamentRepository;
    
    @Transactional(readOnly = true)
    public List<PlayerResponse> getPlayers() {
        List<User> players = userRepository.findActivePlayersOrderByCreatedAt();
        
        // 현재 사용자의 권한 확인
        String currentUserRole = getCurrentUserRole();
        
        if ("ADMIN".equals(currentUserRole)) {
            return players.stream()
                    .map(PlayerResponse::from)
                    .collect(Collectors.toList());
        } else {
            return players.stream()
                    .map(PlayerResponse::fromPublic)
                    .collect(Collectors.toList());
        }
    }
    
    public void deletePlayer(Long playerId) {
        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException());
        
        if (player.getRole() != User.Role.PLAYER) {
            throw new InvalidPlayerException("관리자는 삭제할 수 없습니다");
        }
        
        player.setIsDeleted(true);
        userRepository.save(player);
        
        log.info("Player deleted: {}", player.getNickname());
    }
    
    public void deleteAllPlayers() {
        userRepository.softDeleteAllPlayers();
        log.info("All players deleted");
    }
    
    public void cancelMyParticipation() {
        String currentNickname = getCurrentUserNickname();
        log.debug("Current user nickname: {}", currentNickname);
        
        if (currentNickname == null) {
            throw new UnauthorizedException("로그인이 필요합니다");
        }
        
        // 진행 중인 토너먼트가 있는지 확인
        boolean hasActiveTournament = tournamentRepository.existsByStatus(Tournament.Status.IN_PROGRESS);
        log.debug("Has active tournament: {}", hasActiveTournament);
        
        if (hasActiveTournament) {
            throw new TournamentInProgressException("진행 중인 대회가 있어 참가 취소할 수 없습니다");
        }
        
        User player = userRepository.findByNicknameAndIsDeletedFalse(currentNickname)
                .orElseThrow(() -> new PlayerNotFoundException());
        
        log.debug("Found player: {} with role: {}", player.getNickname(), player.getRole());
        
        if (player.getRole() != User.Role.PLAYER) {
            throw new InvalidPlayerException("관리자는 참가 취소할 수 없습니다");
        }
        
        player.setIsDeleted(true);
        userRepository.save(player);
        
        log.info("Player canceled participation: {}", player.getNickname());
    }
    
    private String getCurrentUserNickname() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getName().equals("anonymousUser")) {
            return null;
        }
        
        return authentication.getName();
    }
    
    private String getCurrentUserRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증되지 않은 사용자이거나 익명 사용자인 경우
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getName().equals("anonymousUser")) {
            return "ANONYMOUS";
        }
        
        // 권한이 없는 경우
        if (authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            return "ANONYMOUS";
        }
        
        // 첫 번째 권한에서 ROLE_ 접두사 제거
        return authentication.getAuthorities().iterator().next()
                .getAuthority().replace("ROLE_", "");
    }
    
    public static class PlayerNotFoundException extends BusinessException {
        public PlayerNotFoundException() {
            super("플레이어를 찾을 수 없습니다");
        }
    }
    
    public static class InvalidPlayerException extends BusinessException {
        public InvalidPlayerException(String message) {
            super(message);
        }
    }
    
    public static class UnauthorizedException extends BusinessException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
    
    public static class TournamentInProgressException extends BusinessException {
        public TournamentInProgressException(String message) {
            super(message);
        }
    }
}
