package com.familyconnect.api.dto;

public record RoomResponse(Long id, String familyId, String name, String roomCode, String status, String createdAt) {
}
