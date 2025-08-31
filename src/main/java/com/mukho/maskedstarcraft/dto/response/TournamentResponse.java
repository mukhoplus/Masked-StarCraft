package com.mukho.maskedstarcraft.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TournamentResponse {
    private Long id;
    private String status;
    private CurrentGameResponse currentGame;
    private List<GameLogResponse> previousGames;
    private TournamentResultResponse result;
    
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
    }
    
    @Data
    @Builder
    public static class TournamentResultResponse {
        private PlayerResponse winner;
        private PlayerResponse maxStreakPlayer;
    }
}
