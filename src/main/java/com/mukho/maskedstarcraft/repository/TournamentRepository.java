package com.mukho.maskedstarcraft.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.mukho.maskedstarcraft.entity.Tournament;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    
    @Query("SELECT t FROM Tournament t WHERE t.status IN ('PREPARING', 'IN_PROGRESS') ORDER BY t.createdAt DESC")
    Optional<Tournament> findCurrentTournament();
    
    @Query("SELECT t FROM Tournament t WHERE t.status = 'FINISHED' ORDER BY t.createdAt DESC")
    List<Tournament> findFinishedTournaments(Pageable pageable);
    
    boolean existsByStatus(Tournament.Status status);
    
    default Optional<Tournament> findLatestFinishedTournament() {
        List<Tournament> tournaments = findFinishedTournaments(Pageable.ofSize(1));
        return tournaments.isEmpty() ? Optional.empty() : Optional.of(tournaments.get(0));
    }
}
