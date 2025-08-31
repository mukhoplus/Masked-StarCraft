package com.mukho.maskedstarcraft.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mukho.maskedstarcraft.entity.GameLog;
import com.mukho.maskedstarcraft.entity.Tournament;

@Repository
public interface GameLogRepository extends JpaRepository<GameLog, Long> {
    
    List<GameLog> findByTournamentOrderByRoundDesc(Tournament tournament);
    
    @Query("SELECT gl FROM GameLog gl WHERE gl.tournament = :tournament ORDER BY gl.round ASC")
    List<GameLog> findByTournamentOrderByRoundAsc(@Param("tournament") Tournament tournament);
    
    @Query("SELECT COUNT(gl) FROM GameLog gl WHERE gl.tournament = :tournament")
    Long countByTournament(@Param("tournament") Tournament tournament);
}
