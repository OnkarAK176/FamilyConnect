package com.familyconnect.api.controller;

import com.familyconnect.api.dto.AcceptInviteRequest;
import com.familyconnect.api.dto.AdminRegistrationRequest;
import com.familyconnect.api.dto.InviteRequest;
import com.familyconnect.api.dto.InviteResponse;
import com.familyconnect.api.dto.MemberResponse;
import com.familyconnect.api.service.FamilyAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FamilyMembershipController {
    private final FamilyAuthService familyAuthService;

    public FamilyMembershipController(FamilyAuthService familyAuthService) {
        this.familyAuthService = familyAuthService;
    }

    @PostMapping("/family/admins")
    public ResponseEntity<MemberResponse> createFamilyAdmin(@RequestBody AdminRegistrationRequest request) {
        return ResponseEntity.ok(familyAuthService.createFamilyAdmin(request));
    }

    @PostMapping("/family/invite")
    public ResponseEntity<InviteResponse> createInvite(@RequestBody InviteRequest request) {
        return ResponseEntity.ok(familyAuthService.createInvite(request));
    }

    @PostMapping("/family/invite/accept")
    public ResponseEntity<MemberResponse> acceptInvite(@RequestBody AcceptInviteRequest request) {
        return ResponseEntity.ok(familyAuthService.acceptInvite(request));
    }

    @GetMapping("/family/{familyId}/members")
    public ResponseEntity<List<MemberResponse>> members(@PathVariable String familyId) {
        return ResponseEntity.ok(familyAuthService.findMembersByFamily(familyId));
    }
}
