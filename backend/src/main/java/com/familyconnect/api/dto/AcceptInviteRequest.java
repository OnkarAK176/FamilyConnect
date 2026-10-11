package com.familyconnect.api.dto;

public record AcceptInviteRequest(String token, String email, String fullName) {
}
