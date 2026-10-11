package com.familyconnect.api.controller;

import com.familyconnect.api.dto.CreateRoomRequest;
import com.familyconnect.api.dto.JoinRoomRequest;
import com.familyconnect.api.dto.RoomResponse;
import com.familyconnect.api.dto.TokenResponse;
import com.familyconnect.api.service.FamilyAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RoomController {
    private final FamilyAuthService familyAuthService;

    public RoomController(FamilyAuthService familyAuthService) {
        this.familyAuthService = familyAuthService;
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(familyAuthService.createRoom(request));
    }

    @PostMapping("/rooms/token")
    public ResponseEntity<TokenResponse> createToken(@RequestBody JoinRoomRequest request) {
        return ResponseEntity.ok(familyAuthService.createJoinToken(request));
    }
}
