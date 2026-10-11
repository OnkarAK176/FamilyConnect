package com.familyconnect.api.dto;

public record TokenResponse(String roomCode, String familyId, String email, String token, String expiresAt) {
}
