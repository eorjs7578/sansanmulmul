package com.sansantek.sansanmulmul.crew.repository;

import com.sansantek.sansanmulmul.crew.domain.Crew;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrewRepository extends JpaRepository<Crew, Integer> {
    Optional<Crew> findByCrewId(int crewId);
    boolean existsByCrewIdAndLeader_UserId(int crewId, int userId);
    List<Crew> findAllByOrderByCrewCreatedAtDesc();
}
