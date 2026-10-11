package com.familyconnect.api.dto;

public record CreateRoomRequest(String familyId, String name, String adminEmail) {
}
