package com.familyconnect.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LiveKitService {
    private static final int MAX_PARTICIPANTS = 20;
    private static final long PARTICIPANT_TOKEN_TTL_SECONDS = 2 * 60 * 60;
    private static final long ADMIN_TOKEN_TTL_SECONDS = 60;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String serverUrl;
    private final String apiKey;
    private final String apiSecret;

    public LiveKitService(
        ObjectMapper objectMapper,
        @Value("${familyconnect.livekit.url:}") String serverUrl,
        @Value("${familyconnect.livekit.api-key:}") String apiKey,
        @Value("${familyconnect.livekit.api-secret:}") String apiSecret
    ) {
        this.objectMapper = objectMapper;
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
    }

    public com.familyconnect.api.dto.CallTokenResponse createCallToken(String roomName, String participantName) {
        validateConfiguration();
        if (roomName == null || !roomName.matches("[a-f0-9]{32}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The call link is invalid.");
        }
        String name = participantName == null ? "" : participantName.trim();
        if (name.isBlank() || name.length() > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a name up to 60 characters long.");
        }

        ensureRoomExists(roomName);
        String identity = UUID.randomUUID().toString();
        String token = createParticipantToken(apiKey, apiSecret, roomName, identity, name, Instant.now());
        return new com.familyconnect.api.dto.CallTokenResponse(serverUrl, token, identity);
    }

    static String createParticipantToken(
        String apiKey,
        String apiSecret,
        String roomName,
        String identity,
        String participantName,
        Instant now
    ) {
        long issuedAt = now.getEpochSecond();
        Map<String, Object> claims = Map.of(
            "iss", apiKey,
            "sub", identity,
            "name", participantName,
            "nbf", issuedAt - 5,
            "iat", issuedAt,
            "exp", issuedAt + PARTICIPANT_TOKEN_TTL_SECONDS,
            "video", Map.of(
                "room", roomName,
                "roomJoin", true,
                "canPublish", true,
                "canSubscribe", true,
                "canPublishData", true
            )
        );
        return signJwt(claims, apiSecret);
    }

    private void ensureRoomExists(String roomName) {
        JsonNode rooms = callRoomService(
            "ListRooms",
            Map.of("names", List.of(roomName)),
            Map.of("roomList", true)
        ).get("rooms");
        if (rooms != null && !rooms.isEmpty()) return;

        try {
            callRoomService(
                "CreateRoom",
                roomCreationRequest(roomName),
                Map.of("roomCreate", true)
            );
        } catch (ResponseStatusException creationFailure) {
            JsonNode roomsAfterCreate = callRoomService(
                "ListRooms",
                Map.of("names", List.of(roomName)),
                Map.of("roomList", true)
            ).get("rooms");
            if (roomsAfterCreate == null || roomsAfterCreate.isEmpty()) throw creationFailure;
        }
    }

    static Map<String, Object> roomCreationRequest(String roomName) {
        return Map.of("name", roomName, "maxParticipants", MAX_PARTICIPANTS, "emptyTimeout", 3600);
    }

    private JsonNode callRoomService(String method, Map<String, ?> requestBody, Map<String, ?> grants) {
        try {
            String adminToken = createAdminToken(grants, Instant.now());
            URI endpoint = roomServiceUri(method);
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "The video service could not prepare the call room. Please try again."
                );
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Call setup was interrupted. Please retry.");
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The video service is unavailable. Please retry.");
        }
    }

    private String createAdminToken(Map<String, ?> grants, Instant now) {
        long issuedAt = now.getEpochSecond();
        Map<String, Object> claims = Map.of(
            "iss", apiKey,
            "sub", apiKey,
            "nbf", issuedAt - 5,
            "iat", issuedAt,
            "exp", issuedAt + ADMIN_TOKEN_TTL_SECONDS,
            "video", grants
        );
        return signJwt(claims, apiSecret);
    }

    private URI roomServiceUri(String method) {
        String httpUrl = serverUrl.replaceFirst("^wss://", "https://").replaceFirst("^ws://", "http://");
        return URI.create(httpUrl.replaceAll("/+$", "") + "/twirp/livekit.RoomService/" + method);
    }

    private void validateConfiguration() {
        if (serverUrl.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Group calling is not configured yet. The site owner needs to finish the video-service setup."
            );
        }
        if (!serverUrl.startsWith("wss://")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The video-service URL must use secure WebSockets.");
        }
    }

    private static String signJwt(Map<String, Object> claims, String secret) {
        try {
            byte[] header = Base64.getUrlEncoder().withoutPadding()
                .encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            byte[] payload = Base64.getUrlEncoder().withoutPadding()
                .encode(new ObjectMapper().writeValueAsString(claims).getBytes(StandardCharsets.UTF_8));
            String signingInput = new String(header, StandardCharsets.US_ASCII) + "."
                + new String(payload, StandardCharsets.US_ASCII);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));
            return signingInput + "." + signature;
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the call access token.");
        }
    }
}
