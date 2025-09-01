package com.mukho.maskedstarcraft.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mukho.maskedstarcraft.dto.request.GameResultRequest;
import com.mukho.maskedstarcraft.dto.response.MapResponse;
import com.mukho.maskedstarcraft.dto.response.PlayerResponse;
import com.mukho.maskedstarcraft.dto.response.TournamentResponse;
import com.mukho.maskedstarcraft.entity.GameLog;
import com.mukho.maskedstarcraft.entity.Tournament;
import com.mukho.maskedstarcraft.entity.User;
import com.mukho.maskedstarcraft.exception.BusinessException;
import com.mukho.maskedstarcraft.repository.GameLogRepository;
import com.mukho.maskedstarcraft.repository.MapRepository;
import com.mukho.maskedstarcraft.repository.TournamentRepository;
import com.mukho.maskedstarcraft.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        webSocketService.broadcastTournamentStart();
        webSocketService.broadcastRefreshRequired();
        
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
        webSocketService.broadcastRefreshRequired();
        
        return getCurrentTournament();
    }
    
    private void createFirstGame(Tournament tournament, List<User> players, List<com.mukho.maskedstarcraft.entity.Map> maps) {
        // 플레이어 리스트를 셔플하여 무작위 순서로 만듦
        List<User> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);
        
        // 첫 두 명의 플레이어 선택 (무작위)
        User player1 = shuffledPlayers.get(0);
        User player2 = shuffledPlayers.get(1);
        
        // 플레이어 순서도 무작위로 결정
        if (new Random().nextBoolean()) {
            User temp = player1;
            player1 = player2;
            player2 = temp;
        }
        
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
        log.info("First game created: {} vs {} on {}", player1.getNickname(), player2.getNickname(), selectedMap.getName());
    }
    
    private void processNextGame(Tournament tournament, User winner) {
        List<User> players = userRepository.findActivePlayersOrderByCreatedAt();
        List<com.mukho.maskedstarcraft.entity.Map> maps = mapRepository.findAllActiveMaps();
        List<GameLog> gameLogs = gameLogRepository.findByTournamentOrderByRoundAsc(tournament);
        
        // 다음 도전자 찾기 (무작위 선택)
        Optional<User> nextChallenger = findNextChallenger(winner, players, gameLogs);
        
        if (nextChallenger.isPresent()) {
            // 다음 경기 생성
            int nextRound = gameLogs.size() + 1;
            com.mukho.maskedstarcraft.entity.Map selectedMap = maps.get(new Random().nextInt(maps.size()));
            
            // 승자와 도전자의 순서도 무작위로 결정
            User player1 = winner;
            User player2 = nextChallenger.get();
            
            if (new Random().nextBoolean()) {
                player1 = nextChallenger.get();
                player2 = winner;
            }
            
            GameLog nextGame = GameLog.builder()
                    .tournament(tournament)
                    .map(selectedMap)
                    .player1(player1)
                    .player2(player2)
                    .round(nextRound)
                    .build();
            
            gameLogRepository.save(nextGame);
            log.info("Next game created: {} vs {} on {} (Round {})", 
                    player1.getNickname(), player2.getNickname(), selectedMap.getName(), nextRound);
        } else {
            // 대회 종료
            finishTournament(tournament, winner, gameLogs);
        }
    }
    
    private void finishTournament(Tournament tournament, User finalWinner, List<GameLog> gameLogs) {
        // 최다 연승자 계산
        MaxStreakResult maxStreakResult = calculateMaxStreakPlayers(gameLogs);
        
        tournament.setStatus(Tournament.Status.FINISHED);
        tournament.setWinnerUser(finalWinner);
        // 첫 번째 최다연승자만 저장 (기존 DB 구조 유지)
        tournament.setMaxStreakUser(maxStreakResult.getPlayers().isEmpty() ? null : maxStreakResult.getPlayers().get(0));
        
        tournamentRepository.save(tournament);
        
        log.info("Tournament finished. Winner: {}, Max streak players: {}", 
                finalWinner.getNickname(), 
                maxStreakResult.getPlayers().stream()
                        .map(User::getNickname)
                        .collect(Collectors.joining(", ")));
                        
        // WebSocket으로 대회 종료 알림
        webSocketService.broadcastTournamentFinish(finalWinner.getNickname());
        webSocketService.broadcastRefreshRequired();
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
        
        // 아직 경기하지 않은 플레이어들을 모두 찾기
        List<User> availablePlayers = players.stream()
                .filter(player -> !player.getId().equals(currentWinner.getId()))
                .filter(player -> !playedPlayerIds.contains(player.getId()))
                .collect(Collectors.toList());
        
        if (availablePlayers.isEmpty()) {
            return Optional.empty();
        }
        
        // 무작위로 한 명 선택
        Collections.shuffle(availablePlayers);
        return Optional.of(availablePlayers.get(0));
    }
    
    private MaxStreakResult calculateMaxStreakPlayers(List<GameLog> gameLogs) {
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
            return new MaxStreakResult(new ArrayList<>(), 0);
        }
        
        int maxStreak = maxStreaks.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        
        List<User> maxStreakPlayers = maxStreaks.entrySet().stream()
                .filter(entry -> entry.getValue() == maxStreak)
                .map(entry -> userRepository.findById(entry.getKey()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return new MaxStreakResult(maxStreakPlayers, maxStreak);
    }
    
    private static class MaxStreakResult {
        private final List<User> players;
        private final int maxStreak;
        
        public MaxStreakResult(List<User> players, int maxStreak) {
            this.players = players;
            this.maxStreak = maxStreak;
        }
        
        public List<User> getPlayers() { return players; }
        public int getMaxStreak() { return maxStreak; }
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
                .showPreviousGames(false) // 진행 중인 대회는 기본적으로 이전 게임 목록을 숨김
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
            // 최다연승자들과 연승수 계산
            List<GameLog> gameLogsAsc = gameLogRepository.findByTournamentOrderByRoundAsc(tournament);
            MaxStreakResult maxStreakResult = calculateMaxStreakPlayers(gameLogsAsc);
            
            // 우승자의 연승수 계산
            int winnerStreak = calculateWinStreak(tournament.getWinnerUser(), gameLogsAsc);
            
            result = TournamentResponse.TournamentResultResponse.builder()
                    .winner(createPlayerResponse(tournament.getWinnerUser()))
                    .winnerStreak(winnerStreak)
                    .maxStreakPlayers(maxStreakResult.getPlayers().stream()
                            .map(this::createPlayerResponse)
                            .collect(Collectors.toList()))
                    .maxStreak(maxStreakResult.getMaxStreak())
                    .build();
        }
        
        return TournamentResponse.builder()
                .id(tournament.getId())
                .status(tournament.getStatus().name())
                .previousGames(gameLogResponses)
                .result(result)
                .showPreviousGames(true) // 종료된 대회는 이전 게임 목록을 표시
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
        // 임시로 항상 이름을 포함하도록 수정 (디버깅용)
        return PlayerResponse.from(user);
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
