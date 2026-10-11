package com.familyconnect.api.repository;

import com.familyconnect.api.domain.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {
    Optional<MeetingRoom> findByRoomCode(String roomCode);

    List<MeetingRoom> findByFamilyId(String familyId);
}
