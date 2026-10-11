package com.familyconnect.api.repository;

import com.familyconnect.api.domain.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    Optional<FamilyMember> findByEmail(String email);

    List<FamilyMember> findByFamilyId(String familyId);

    boolean existsByFamilyIdAndEmail(String familyId, String email);
}
