package com.mukho.maskedstarcraft.service;

import com.mukho.maskedstarcraft.dto.response.*;
import com.mukho.maskedstarcraft.entity.GameLog;
import com.mukho.maskedstarcraft.entity.Tournament;
import com.mukho.maskedstarcraft.entity.User;
import com.mukho.maskedstarcraft.exception.BusinessException;
import com.mukho.maskedstarcraft.repository.GameLogRepository;
import com.mukho.maskedstarcraft.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class LogService {
    
    private final TournamentRepository tournamentRepository;
    private final GameLogRepository gameLogRepository;
    
    public List<TournamentLogResponse> getAllTournamentLogs() {
        List<Tournament> tournaments = tournamentRepository.findAll()
                .stream()
                .filter(t -> t.getStatus() == Tournament.Status.FINISHED)
                .collect(Collectors.toList());
        
        return tournaments.stream()
                .map(this::convertToLogResponse)
                .collect(Collectors.toList());
    }
    
    public TournamentLogResponse getTournamentLog(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException());
        
        if (tournament.getStatus() != Tournament.Status.FINISHED) {
            throw new InvalidTournamentStatusException("완료된 대회만 로그를 조회할 수 있습니다");
        }
        
        return convertToLogResponse(tournament);
    }
    
    public ResponseEntity<Resource> downloadTournamentLog(Long tournamentId) {
        TournamentLogResponse logData = getTournamentLog(tournamentId);
        
        String logContent = generateLogContent(logData);
        ByteArrayResource resource = new ByteArrayResource(logContent.getBytes());
        
        String filename = String.format("tournament_%d_%s.txt", 
                tournamentId, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(resource.contentLength())
                .body(resource);
    }
    
    private TournamentLogResponse convertToLogResponse(Tournament tournament) {
        List<GameLog> gameLogs = gameLogRepository.findByTournamentOrderByRoundAsc(tournament);
        
        List<TournamentLogResponse.GameLogDetail> gameDetails = gameLogs.stream()
                .filter(game -> game.getWinner() != null)
                .map(game -> {
                    int streak = calculateWinStreakAtGame(game, gameLogs);
                    return TournamentLogResponse.GameLogDetail.builder()
                            .round(game.getRound())
                            .player1(createPlayerResponse(game.getPlayer1()))
                            .player2(createPlayerResponse(game.getPlayer2()))
                            .winner(createPlayerResponse(game.getWinner()))
                            .map(MapResponse.from(game.getMap()))
                            .playTime(game.getCreatedAt())
                            .winnerStreak(streak)
                            .build();
                })
                .collect(Collectors.toList());
        
        // 통계 계산
        long totalParticipants = gameLogs.stream()
                .flatMap(game -> java.util.stream.Stream.of(game.getPlayer1(), game.getPlayer2()))
                .map(User::getId)
                .distinct()
                .count();
        
        int maxStreak = gameDetails.stream()
                .mapToInt(TournamentLogResponse.GameLogDetail::getWinnerStreak)
                .max()
                .orElse(0);
        
        String duration = calculateDuration(tournament.getCreatedAt(), 
                gameLogs.isEmpty() ? tournament.getCreatedAt() : 
                gameLogs.get(gameLogs.size() - 1).getCreatedAt());
        
        TournamentLogResponse.TournamentStats stats = TournamentLogResponse.TournamentStats.builder()
                .totalGames(gameDetails.size())
                .totalParticipants((int) totalParticipants)
                .maxStreak(maxStreak)
                .duration(duration)
                .build();
        
        // 최다연승자들 계산
        List<PlayerResponse> maxStreakPlayers = calculateMaxStreakPlayersFromGames(gameLogs);
        
        // 우승자 연승 계산
        Integer winnerStreak = null;
        if (tournament.getWinnerUser() != null) {
            winnerStreak = calculateWinStreak(tournament.getWinnerUser(), gameLogs);
        }
        
        return TournamentLogResponse.builder()
                .tournamentId(tournament.getId())
                .startTime(tournament.getCreatedAt())
                .endTime(gameLogs.isEmpty() ? null : gameLogs.get(gameLogs.size() - 1).getCreatedAt())
                .status(tournament.getStatus().name())
                .winner(tournament.getWinnerUser() != null ? createPlayerResponse(tournament.getWinnerUser()) : null)
                .winnerStreak(winnerStreak)
                .maxStreakPlayer(tournament.getMaxStreakUser() != null ? createPlayerResponse(tournament.getMaxStreakUser()) : null)
                .maxStreakPlayers(maxStreakPlayers)
                .maxStreak(maxStreak)
                .games(gameDetails)
                .stats(stats)
                .build();
    }
    
    private List<PlayerResponse> calculateMaxStreakPlayersFromGames(List<GameLog> gameLogs) {
        java.util.Map<Long, Integer> maxStreaks = new HashMap<>();
        java.util.Map<Long, Integer> currentStreaks = new HashMap<>();
        
        for (GameLog game : gameLogs) {
            if (game.getWinner() != null) {
                Long winnerId = game.getWinner().getId();
                currentStreaks.put(winnerId, currentStreaks.getOrDefault(winnerId, 0) + 1);
                maxStreaks.put(winnerId, Math.max(maxStreaks.getOrDefault(winnerId, 0), 
                                                 currentStreaks.get(winnerId)));
                
                // 패배자의 연승 초기화
                Long loserId = game.getPlayer1().getId().equals(winnerId) ? 
                              game.getPlayer2().getId() : game.getPlayer1().getId();
                currentStreaks.put(loserId, 0);
            }
        }
        
        if (maxStreaks.isEmpty()) {
            return new ArrayList<>();
        }
        
        int maxStreak = maxStreaks.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        
        return maxStreaks.entrySet().stream()
                .filter(entry -> entry.getValue() == maxStreak)
                .map(entry -> gameLogs.stream()
                        .flatMap(game -> java.util.stream.Stream.of(game.getPlayer1(), game.getPlayer2()))
                        .filter(user -> user.getId().equals(entry.getKey()))
                        .findFirst()
                        .map(this::createPlayerResponse)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    private int calculateWinStreak(User player, List<GameLog> gameLogs) {
        int streak = 0;
        for (int i = gameLogs.size() - 1; i >= 0; i--) {
            GameLog game = gameLogs.get(i);
            if (game.getWinner() != null && game.getWinner().getId().equals(player.getId())) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
    
    private int calculateWinStreakAtGame(GameLog targetGame, List<GameLog> allGames) {
        if (targetGame.getWinner() == null) return 0;
        
        int streak = 0;
        for (GameLog game : allGames) {
            if (game.getRound() > targetGame.getRound()) break;
            if (game.getWinner() == null) continue;
            
            if (game.getWinner().getId().equals(targetGame.getWinner().getId())) {
                streak++;
            } else if (game.getPlayer1().getId().equals(targetGame.getWinner().getId()) ||
                      game.getPlayer2().getId().equals(targetGame.getWinner().getId())) {
                streak = 0; // 패배 시 연승 초기화
            }
        }
        return streak;
    }
    
    private String calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return "N/A";
        
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        
        if (hours > 0) {
            return String.format("%d시간 %d분", hours, minutes);
        } else {
            return String.format("%d분", minutes);
        }
    }
    
    private String generateLogContent(TournamentLogResponse logData) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=".repeat(80)).append("\n");
        sb.append("복면스타왕 대회 로그\n");
        sb.append("=".repeat(80)).append("\n\n");
        
        // 대회 정보
        sb.append("📋 대회 정보\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append(String.format("대회 ID: %d\n", logData.getTournamentId()));
        sb.append(String.format("시작 시간: %s\n", formatDateTime(logData.getStartTime())));
        sb.append(String.format("종료 시간: %s\n", formatDateTime(logData.getEndTime())));
        sb.append(String.format("진행 시간: %s\n", logData.getStats().getDuration()));
        sb.append(String.format("상태: %s\n\n", logData.getStatus()));
        
        // 결과 정보
        sb.append("🏆 대회 결과\n");
        sb.append("-".repeat(40)).append("\n");
        if (logData.getWinner() != null) {
            String winnerText = logData.getWinner().getName() != null ? 
                    String.format("%s(%s)", logData.getWinner().getNickname(), logData.getWinner().getName()) :
                    logData.getWinner().getNickname();
            int winnerStreak = logData.getWinnerStreak() != null ? logData.getWinnerStreak().intValue() : 0;
            sb.append(String.format("최종 우승자: %s (%d연승)\n", winnerText, winnerStreak));
        }
        
        if (logData.getMaxStreakPlayers() != null && !logData.getMaxStreakPlayers().isEmpty()) {
            int maxStreak = logData.getMaxStreak() != null ? logData.getMaxStreak().intValue() : 0;
            sb.append(String.format("최다 연승자(%d연승):\n", maxStreak));
            for (PlayerResponse player : logData.getMaxStreakPlayers()) {
                String playerText = player.getName() != null ? 
                        String.format("%s(%s)", player.getNickname(), player.getName()) :
                        player.getNickname();
                sb.append(String.format("  - %s\n", playerText));
            }
        }
        sb.append("\n");
        
        // 통계 정보
        sb.append("📊 대회 통계\n");
        sb.append("-".repeat(40)).append("\n");
        sb.append(String.format("총 경기 수: %d경기\n", logData.getStats().getTotalGames()));
        sb.append(String.format("참가자 수: %d명\n", logData.getStats().getTotalParticipants()));
        sb.append(String.format("최대 연승: %d연승\n\n", logData.getStats().getMaxStreak()));
        
        // 경기 상세
        sb.append("🎮 경기 상세\n");
        sb.append("-".repeat(80)).append("\n");
        sb.append(String.format("%-5s %-15s %-15s %-15s %-15s %s\n", 
                "라운드", "플레이어1", "플레이어2", "승자", "맵", "연승"));
        sb.append("-".repeat(80)).append("\n");
        
        for (TournamentLogResponse.GameLogDetail game : logData.getGames()) {
            sb.append(String.format("%-5d %-15s %-15s %-15s %-15s %d연승\n",
                    game.getRound(),
                    formatPlayer(game.getPlayer1()),
                    formatPlayer(game.getPlayer2()),
                    formatPlayer(game.getWinner()),
                    game.getMap().getName(),
                    game.getWinnerStreak()));
        }
        
        sb.append("\n");
        sb.append("=".repeat(80)).append("\n");
        sb.append(String.format("로그 생성 시간: %s\n", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        sb.append("=".repeat(80));
        
        return sb.toString();
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private String formatPlayer(PlayerResponse player) {
        if (player == null) return "N/A";
        if (player.getName() != null && !player.getName().isEmpty()) {
            return String.format("%s(%s)", player.getNickname(), player.getName());
        }
        return player.getNickname();
    }
    
    private PlayerResponse createPlayerResponse(User user) {
        // 임시로 항상 이름을 포함하도록 수정 (디버깅용)
        return PlayerResponse.from(user);
    }
    
    private String getCurrentUserRole() {
        try {
            return SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        } catch (Exception e) {
            return "PLAYER";
        }
    }
    
    public static class TournamentNotFoundException extends BusinessException {
        public TournamentNotFoundException() {
            super("대회를 찾을 수 없습니다");
        }
    }
    
    public static class InvalidTournamentStatusException extends BusinessException {
        public InvalidTournamentStatusException(String message) {
            super(message);
        }
    }
}
