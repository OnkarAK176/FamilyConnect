package com.familyconnect.api.dto;

public record InviteResponse(String familyId, String token, String createdByEmail, String expiresAt) {
}
