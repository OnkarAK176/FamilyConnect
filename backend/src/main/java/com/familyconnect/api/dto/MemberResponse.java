package com.familyconnect.api.dto;

public record MemberResponse(String familyId, String email, String fullName, String role, String status) {
}
