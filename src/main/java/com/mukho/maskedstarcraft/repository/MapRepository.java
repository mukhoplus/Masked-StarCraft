package com.mukho.maskedstarcraft.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.mukho.maskedstarcraft.entity.Map;

@Repository
public interface MapRepository extends JpaRepository<Map, Long> {
    
    @Query("SELECT m FROM Map m WHERE m.isDeleted = false ORDER BY m.createdAt ASC")
    List<Map> findAllActiveMaps();
    
    Optional<Map> findByIdAndIsDeletedFalse(Long id);
    
    boolean existsByNameAndIsDeletedFalse(String name);
}
