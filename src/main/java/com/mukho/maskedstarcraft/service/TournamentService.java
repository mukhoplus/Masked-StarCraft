package com.mukho.maskedstarcraft.service;

import com.mukho.maskedstarcraft.dto.request.GameResultRequest;
import com.mukho.maskedstarcraft.dto.response.PlayerResponse;
import com.mukho.maskedstarcraft.dto.response.TournamentResponse;
import com.mukho.maskedstarcraft.dto.response.MapResponse;
import com.mukho.maskedstarcraft.entity.*;
import com.mukho.maskedstarcraft.exception.BusinessException;
import com.mukho.maskedstarcraft.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TournamentService {
    
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final MapRepository mapRepository;
    private final GameLogRepository gameLogRepository;
    private final WebSocketService webSocketService;
    
    public TournamentResponse startTournament() {
        // 진행 중인 토너먼트 체크
        Optional<Tournament> currentTournament = tournamentRepository.findCurrentTournament();
        if (currentTournament.isPresent() && 
            currentTournament.get().getStatus() == Tournament.Status.IN_PROGRESS) {
            throw new TournamentAlreadyInProgressException();
        }
        
        // 참가자 수 체크 (최소 2명)
        List<User> players = userRepository.findActivePlayersOrderByCreatedAt();
        if (players.size() < 2) {
            throw new InsufficientPlayersException();
        }
        
        // 맵 개수 체크 (최소 1개)
        List<com.mukho.maskedstarcraft.entity.Map> maps = mapRepository.findAllActiveMaps();
        if (maps.isEmpty()) {
            throw new InsufficientMapsException();
        }
        
        // 새 토너먼트 생성
        Tournament tournament = Tournament.builder()
                .status(Tournament.Status.IN_PROGRESS)
                .build();
        
        Tournament savedTournament = tournamentRepository.save(tournament);
        
        // 첫 번째 경기 생성
        createFirstGame(savedTournament, players, maps);
        
        log.info("Tournament started with {} players", players.size());
        
        // WebSocket으로 알림
        webSocketService.broadcastTournamentUpdate();
        
        return getCurrentTournament();
    }
    
    @Transactional(readOnly = true)
    public TournamentResponse getCurrentTournament() {
        Optional<Tournament> tournamentOpt = tournamentRepository.findCurrentTournament();
        
        if (tournamentOpt.isEmpty()) {
            // 진행 중인 대회가 없으면 마지막 완료된 대회 결과 반환
            Optional<Tournament> lastFinished = tournamentRepository.findLatestFinishedTournament();
            if (lastFinished.isPresent()) {
                return buildFinishedTournamentResponse(lastFinished.get());
            }
            return null; // 대회 없음
        }
        
        Tournament tournament = tournamentOpt.get();
        
        if (tournament.getStatus() == Tournament.Status.IN_PROGRESS) {
            return buildInProgressTournamentResponse(tournament);
        } else {
            return buildFinishedTournamentResponse(tournament);
        }
    }
    
    public TournamentResponse recordGameResult(GameResultRequest request) {
        Tournament tournament = tournamentRepository.findCurrentTournament()
                .orElseThrow(() -> new TournamentNotFoundException());
        
        if (tournament.getStatus() != Tournament.Status.IN_PROGRESS) {
            throw new InvalidGameResultException("진행 중인 대회가 아닙니다");
        }
        
        // 현재 진행 중인 경기 찾기
        List<GameLog> gameLogs = gameLogRepository.findByTournamentOrderByRoundDesc(tournament);
        if (gameLogs.isEmpty()) {
            throw new InvalidGameResultException("진행 중인 경기가 없습니다");
        }
        
        GameLog currentGame = gameLogs.get(0);
        
        // 승자 검증
        User winner = userRepository.findById(request.getWinnerId())
                .orElseThrow(() -> new InvalidGameResultException("존재하지 않는 사용자입니다"));
        
        if (!winner.getId().equals(currentGame.getPlayer1().getId()) && 
            !winner.getId().equals(currentGame.getPlayer2().getId())) {
            throw new InvalidGameResultException("경기 참가자가 아닙니다");
        }
        
        // 게임 결과 업데이트
        currentGame.setWinner(winner);
        gameLogRepository.save(currentGame);
        
        log.info("Game result recorded: {} wins", winner.getNickname());
        
        // 다음 경기 또는 토너먼트 종료 처리
        processNextGame(tournament, winner);
        
        // WebSocket으로 알림
        webSocketService.broadcastTournamentUpdate();
        
        return getCurrentTournament();
    }
    
    private void createFirstGame(Tournament tournament, List<User> players, List<com.mukho.maskedstarcraft.entity.Map> maps) {
        // 첫 두 명의 플레이어 선택
        User player1 = players.get(0);
        User player2 = players.get(1);
        
        // 랜덤 맵 선택
        com.mukho.maskedstarcraft.entity.Map selectedMap = maps.get(new Random().nextInt(maps.size()));
        
        GameLog gameLog = GameLog.builder()
                .tournament(tournament)
                .map(selectedMap)
                .player1(player1)
                .player2(player2)
                .round(1)
                .build();
        
        gameLogRepository.save(gameLog);
    }
    
    private void processNextGame(Tournament tournament, User winner) {
        List<User> players = userRepository.findActivePlayersOrderByCreatedAt();
        List<com.mukho.maskedstarcraft.entity.Map> maps = mapRepository.findAllActiveMaps();
        List<GameLog> gameLogs = gameLogRepository.findByTournamentOrderByRoundAsc(tournament);
        
        // 연승 계산
        int winStreak = calculateWinStreak(winner, gameLogs);
        
        // 다음 도전자 찾기
        Optional<User> nextChallenger = findNextChallenger(winner, players, gameLogs);
        
        if (nextChallenger.isPresent()) {
            // 다음 경기 생성
            int nextRound = gameLogs.size() + 1;
            com.mukho.maskedstarcraft.entity.Map selectedMap = maps.get(new Random().nextInt(maps.size()));
            
            GameLog nextGame = GameLog.builder()
                    .tournament(tournament)
                    .map(selectedMap)
                    .player1(winner) // 승자가 연승자
                    .player2(nextChallenger.get())
                    .round(nextRound)
                    .build();
            
            gameLogRepository.save(nextGame);
        } else {
            // 대회 종료
            finishTournament(tournament, winner, gameLogs);
        }
    }
    
    private void finishTournament(Tournament tournament, User finalWinner, List<GameLog> gameLogs) {
        // 최다 연승자 계산
        User maxStreakPlayer = calculateMaxStreakPlayer(gameLogs);
        
        tournament.setStatus(Tournament.Status.FINISHED);
        tournament.setWinnerUser(finalWinner);
        tournament.setMaxStreakUser(maxStreakPlayer);
        
        tournamentRepository.save(tournament);
        
        log.info("Tournament finished. Winner: {}, Max streak: {}", 
                finalWinner.getNickname(), 
                maxStreakPlayer.getNickname());
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
    
    private Optional<User> findNextChallenger(User currentWinner, List<User> players, List<GameLog> gameLogs) {
        Set<Long> playedPlayerIds = gameLogs.stream()
                .flatMap(game -> Arrays.stream(new Long[]{game.getPlayer1().getId(), game.getPlayer2().getId()}))
                .collect(Collectors.toSet());
        
        return players.stream()
                .filter(player -> !player.getId().equals(currentWinner.getId()))
                .filter(player -> !playedPlayerIds.contains(player.getId()))
                .findFirst();
    }
    
    private User calculateMaxStreakPlayer(List<GameLog> gameLogs) {
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
        
        Long maxStreakPlayerId = maxStreaks.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
        
        return maxStreakPlayerId != null ? 
               userRepository.findById(maxStreakPlayerId).orElse(null) : null;
    }
    
    private TournamentResponse buildInProgressTournamentResponse(Tournament tournament) {
        List<GameLog> gameLogs = gameLogRepository.findByTournamentOrderByRoundDesc(tournament);
        
        if (gameLogs.isEmpty()) {
            return TournamentResponse.builder()
                    .id(tournament.getId())
                    .status(tournament.getStatus().name())
                    .build();
        }
        
        // 현재 경기 (승자가 없는 마지막 경기)
        GameLog currentGame = gameLogs.stream()
                .filter(game -> game.getWinner() == null)
                .findFirst()
                .orElse(null);
        
        // 이전 경기들 (승자가 있는 경기들)
        List<GameLog> previousGames = gameLogs.stream()
                .filter(game -> game.getWinner() != null)
                .collect(Collectors.toList());
        
        TournamentResponse.CurrentGameResponse currentGameResponse = null;
        if (currentGame != null) {
            currentGameResponse = TournamentResponse.CurrentGameResponse.builder()
                    .player1(createPlayerResponse(currentGame.getPlayer1()))
                    .player2(createPlayerResponse(currentGame.getPlayer2()))
                    .map(MapResponse.from(currentGame.getMap()))
                    .round(currentGame.getRound())
                    .build();
        }
        
        List<TournamentResponse.GameLogResponse> gameLogResponses = previousGames.stream()
                .map(this::createGameLogResponse)
                .collect(Collectors.toList());
        
        return TournamentResponse.builder()
                .id(tournament.getId())
                .status(tournament.getStatus().name())
                .currentGame(currentGameResponse)
                .previousGames(gameLogResponses)
                .build();
    }
    
    private TournamentResponse buildFinishedTournamentResponse(Tournament tournament) {
        List<GameLog> gameLogs = gameLogRepository.findByTournamentOrderByRoundDesc(tournament);
        
        List<TournamentResponse.GameLogResponse> gameLogResponses = gameLogs.stream()
                .filter(game -> game.getWinner() != null)
                .map(this::createGameLogResponse)
                .collect(Collectors.toList());
        
        TournamentResponse.TournamentResultResponse result = null;
        if (tournament.getWinnerUser() != null) {
            result = TournamentResponse.TournamentResultResponse.builder()
                    .winner(createPlayerResponse(tournament.getWinnerUser()))
                    .maxStreakPlayer(tournament.getMaxStreakUser() != null ? 
                                   createPlayerResponse(tournament.getMaxStreakUser()) : null)
                    .build();
        }
        
        return TournamentResponse.builder()
                .id(tournament.getId())
                .status(tournament.getStatus().name())
                .previousGames(gameLogResponses)
                .result(result)
                .build();
    }
    
    private TournamentResponse.GameLogResponse createGameLogResponse(GameLog gameLog) {
        User loser = gameLog.getPlayer1().getId().equals(gameLog.getWinner().getId()) ? 
                    gameLog.getPlayer2() : gameLog.getPlayer1();
        
        int streak = calculateWinStreakAtGame(gameLog);
        
        return TournamentResponse.GameLogResponse.builder()
                .winner(createPlayerResponse(gameLog.getWinner()))
                .loser(createPlayerResponse(loser))
                .map(MapResponse.from(gameLog.getMap()))
                .round(gameLog.getRound())
                .streak(streak)
                .build();
    }
    
    private int calculateWinStreakAtGame(GameLog gameLog) {
        List<GameLog> previousGames = gameLogRepository.findByTournamentOrderByRoundAsc(gameLog.getTournament())
                .stream()
                .filter(game -> game.getRound() <= gameLog.getRound() && game.getWinner() != null)
                .collect(Collectors.toList());
        
        return calculateWinStreak(gameLog.getWinner(), previousGames);
    }
    
    private PlayerResponse createPlayerResponse(User user) {
        String currentUserRole = getCurrentUserRole();
        
        if ("ADMIN".equals(currentUserRole)) {
            return PlayerResponse.from(user);
        } else {
            return PlayerResponse.fromPublic(user);
        }
    }
    
    private String getCurrentUserRole() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
    
    // Exception classes
    public static class TournamentNotFoundException extends BusinessException {
        public TournamentNotFoundException() {
            super("진행 중인 대회가 없습니다");
        }
    }
    
    public static class TournamentAlreadyInProgressException extends BusinessException {
        public TournamentAlreadyInProgressException() {
            super("이미 진행 중인 대회가 있습니다");
        }
    }
    
    public static class InsufficientPlayersException extends BusinessException {
        public InsufficientPlayersException() {
            super("대회를 시작하려면 최소 2명의 참가자가 필요합니다");
        }
    }
    
    public static class InsufficientMapsException extends BusinessException {
        public InsufficientMapsException() {
            super("대회를 시작하려면 최소 1개의 맵이 필요합니다");
        }
    }
    
    public static class InvalidGameResultException extends BusinessException {
        public InvalidGameResultException(String message) {
            super(message);
        }
    }
}
