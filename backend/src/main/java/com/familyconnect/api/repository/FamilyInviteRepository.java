package com.familyconnect.api.repository;

import com.familyconnect.api.domain.FamilyInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FamilyInviteRepository extends JpaRepository<FamilyInvite, Long> {
    Optional<FamilyInvite> findByToken(String token);
}
