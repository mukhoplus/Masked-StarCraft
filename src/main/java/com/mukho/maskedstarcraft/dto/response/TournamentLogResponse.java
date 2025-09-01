package com.mukho.maskedstarcraft.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TournamentLogResponse {
    private Long tournamentId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private PlayerResponse winner;
    private Integer winnerStreak;
    private PlayerResponse maxStreakPlayer; // 호환성을 위해 유지
    private List<PlayerResponse> maxStreakPlayers;
    private Integer maxStreak;
    private List<GameLogDetail> games;
    private TournamentStats stats;
    
    @Data
    @Builder
    public static class GameLogDetail {
        private Integer round;
        private PlayerResponse player1;
        private PlayerResponse player2;
        private PlayerResponse winner;
        private MapResponse map;
        private LocalDateTime playTime;
        private Integer winnerStreak;
    }
    
    @Data
    @Builder
    public static class TournamentStats {
        private Integer totalGames;
        private Integer totalParticipants;
        private Integer maxStreak;
        private String duration;
    }
}
