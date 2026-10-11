package com.familyconnect.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "meeting_rooms")
public class MeetingRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private String familyId;

    @Column(nullable = false)
    private String name;

    @Column(name = "room_code", nullable = false, unique = true)
    private String roomCode;

    @Column(name = "admin_email", nullable = false)
    private String adminEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected MeetingRoom() {
    }

    public MeetingRoom(String familyId, String name, String roomCode, String adminEmail) {
        this.familyId = familyId;
        this.name = name;
        this.roomCode = roomCode;
        this.adminEmail = adminEmail;
    }

    public Long getId() {
        return id;
    }

    public String getFamilyId() {
        return familyId;
    }

    public String getName() {
        return name;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
