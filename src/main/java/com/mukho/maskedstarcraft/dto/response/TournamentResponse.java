package com.mukho.maskedstarcraft.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TournamentResponse {
    private Long id;
    private String status;
    private CurrentGameResponse currentGame;
    private List<GameLogResponse> previousGames;
    private TournamentResultResponse result;
    private Boolean showPreviousGames; // 게임 목록 표시 여부 (UI에서 사용)
    
    @Data
    @Builder
    public static class CurrentGameResponse {
        private PlayerResponse player1;
        private PlayerResponse player2;
        private MapResponse map;
        private Integer round;
    }
    
    @Data
    @Builder
    public static class GameLogResponse {
        private PlayerResponse winner;
        private PlayerResponse loser;
        private MapResponse map;
        private Integer round;
        private Integer streak;
        
        // 편의 메서드들
        public String getWinnerDisplayName() {
            return winner != null ? winner.getDisplayName() : "N/A";
        }
        
        public String getLoserDisplayName() {
            return loser != null ? loser.getDisplayName() : "N/A";
        }
    }
    
    @Data
    @Builder
    public static class TournamentResultResponse {
        private PlayerResponse winner;
        private Integer winnerStreak;
        private List<PlayerResponse> maxStreakPlayers;
        private Integer maxStreak;
        
        // 편의 메서드들
        public String getWinnerDisplayName() {
            return winner != null ? winner.getDisplayName() : "N/A";
        }
        
        public String getWinnerDisplayNameWithStreak() {
            if (winner != null) {
                String streakText = winnerStreak != null ? winnerStreak.toString() + "연승" : "0연승";
                return winner.getDisplayName() + " (" + streakText + ")";
            }
            return "N/A";
        }
        
        public String getMaxStreakPlayersDisplay() {
            if (maxStreakPlayers == null || maxStreakPlayers.isEmpty()) {
                return "N/A";
            }
            String streakText = maxStreak != null ? maxStreak.toString() + "연승" : "0연승";
            String playersText = maxStreakPlayers.stream()
                    .map(PlayerResponse::getDisplayName)
                    .collect(java.util.stream.Collectors.joining(", "));
            return "최다 연승자(" + streakText + "): " + playersText;
        }
    }
}
