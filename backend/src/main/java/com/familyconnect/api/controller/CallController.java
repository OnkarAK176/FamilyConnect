package com.familyconnect.api.controller;

import com.familyconnect.api.dto.CallTokenResponse;
import com.familyconnect.api.dto.CreateCallTokenRequest;
import com.familyconnect.api.service.LiveKitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calls")
public class CallController {
    private final LiveKitService liveKitService;

    public CallController(LiveKitService liveKitService) {
        this.liveKitService = liveKitService;
    }

    @PostMapping("/token")
    public ResponseEntity<CallTokenResponse> createToken(@RequestBody CreateCallTokenRequest request) {
        return ResponseEntity.ok(liveKitService.createCallToken(request.roomName(), request.participantName()));
    }
}
