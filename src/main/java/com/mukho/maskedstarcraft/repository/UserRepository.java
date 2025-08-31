package com.mukho.maskedstarcraft.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.mukho.maskedstarcraft.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByNicknameAndIsDeletedFalse(String nickname);
    
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND u.role = 'PLAYER' ORDER BY u.createdAt ASC")
    List<User> findActivePlayersOrderByCreatedAt();
    
    @Modifying
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.role = 'PLAYER'")
    void softDeleteAllPlayers();
    
    boolean existsByNicknameAndIsDeletedFalse(String nickname);
}
