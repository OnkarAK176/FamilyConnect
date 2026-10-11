package com.familyconnect.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveKitServiceTest {
    @Test
    void createsShortLivedJoinTokenWithRoomScopedPublishAndSubscribeGrants() throws Exception {
        Instant now = Instant.parse("2026-10-11T07:00:00Z");
        String token = LiveKitService.createParticipantToken(
            "test-api-key",
            "test-api-secret",
            "a".repeat(32),
            "participant-id",
            "Onkar",
            now
        );
        String[] parts = token.split("\\.");
        var mapper = new ObjectMapper();
        var header = mapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
        var claims = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));

        assertEquals("HS256", header.get("alg").asText());
        assertEquals("test-api-key", claims.get("iss").asText());
        assertEquals("participant-id", claims.get("sub").asText());
        assertEquals("Onkar", claims.get("name").asText());
        assertEquals(now.getEpochSecond() + 7200, claims.get("exp").asLong());
        assertEquals("a".repeat(32), claims.get("video").get("room").asText());
        assertTrue(claims.get("video").get("roomJoin").asBoolean());
        assertTrue(claims.get("video").get("canPublish").asBoolean());
        assertTrue(claims.get("video").get("canSubscribe").asBoolean());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-api-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(
            mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII))
        );
        assertEquals(expectedSignature, parts[2]);
    }

    @Test
    void reportsMissingVideoProviderConfigurationWithoutIssuingAToken() {
        LiveKitService service = new LiveKitService(new ObjectMapper(), "", "", "");

        ResponseStatusException error = assertThrows(
            ResponseStatusException.class,
            () -> service.createCallToken("a".repeat(32), "Onkar")
        );

        assertEquals(503, error.getStatusCode().value());
        assertTrue(error.getReason().contains("not configured"));
    }

    @Test
    void rejectsInvalidRoomIdsAndEmptyParticipantNames() {
        LiveKitService service = new LiveKitService(new ObjectMapper(), "wss://example.livekit.cloud", "key", "secret");

        ResponseStatusException invalidRoom = assertThrows(
            ResponseStatusException.class,
            () -> service.createCallToken("not-a-room", "Onkar")
        );
        ResponseStatusException invalidName = assertThrows(
            ResponseStatusException.class,
            () -> service.createCallToken("a".repeat(32), "  ")
        );

        assertEquals(400, invalidRoom.getStatusCode().value());
        assertEquals(400, invalidName.getStatusCode().value());
    }

    @Test
    void createsRoomsWithTheFamilyCallParticipantLimit() {
        Map<String, Object> room = LiveKitService.roomCreationRequest("a".repeat(32));

        assertEquals("a".repeat(32), room.get("name"));
        assertEquals(20, room.get("maxParticipants"));
        assertEquals(3600, room.get("emptyTimeout"));
    }
}
