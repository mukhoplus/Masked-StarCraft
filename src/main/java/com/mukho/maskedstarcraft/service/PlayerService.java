package com.mukho.maskedstarcraft.service;

import com.mukho.maskedstarcraft.dto.response.PlayerResponse;
import com.mukho.maskedstarcraft.entity.User;
import com.mukho.maskedstarcraft.exception.BusinessException;
import com.mukho.maskedstarcraft.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PlayerService {
    
    private final UserRepository userRepository;
    
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
    
    private String getCurrentUserRole() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
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
}
