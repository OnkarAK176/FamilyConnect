package com.familyconnect.api.service;

import com.familyconnect.api.domain.FamilyInvite;
import com.familyconnect.api.domain.FamilyMember;
import com.familyconnect.api.domain.MeetingRoom;
import com.familyconnect.api.domain.MemberRole;
import com.familyconnect.api.domain.MembershipStatus;
import com.familyconnect.api.domain.RoomStatus;
import com.familyconnect.api.dto.AcceptInviteRequest;
import com.familyconnect.api.dto.AdminRegistrationRequest;
import com.familyconnect.api.dto.CreateRoomRequest;
import com.familyconnect.api.dto.InviteRequest;
import com.familyconnect.api.dto.InviteResponse;
import com.familyconnect.api.dto.JoinRoomRequest;
import com.familyconnect.api.dto.MemberResponse;
import com.familyconnect.api.dto.RoomResponse;
import com.familyconnect.api.dto.TokenResponse;
import com.familyconnect.api.repository.FamilyInviteRepository;
import com.familyconnect.api.repository.FamilyMemberRepository;
import com.familyconnect.api.repository.MeetingRoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FamilyAuthService {
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyInviteRepository familyInviteRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final String tokenSecret;

    public FamilyAuthService(
        FamilyMemberRepository familyMemberRepository,
        FamilyInviteRepository familyInviteRepository,
        MeetingRoomRepository meetingRoomRepository,
        @Value("${familyconnect.token.secret:family-connect-demo-secret}") String tokenSecret
    ) {
        this.familyMemberRepository = familyMemberRepository;
        this.familyInviteRepository = familyInviteRepository;
        this.meetingRoomRepository = meetingRoomRepository;
        this.tokenSecret = tokenSecret;
    }

    @Transactional
    public MemberResponse createFamilyAdmin(AdminRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        String fullName = request.fullName() == null ? "Family member" : request.fullName().trim();
        if (email.isBlank() || fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and full name are required.");
        }

        Optional<FamilyMember> existing = familyMemberRepository.findByEmail(email);
        if (existing.isPresent()) {
            FamilyMember member = existing.get();
            return mapMember(member);
        }

        String familyId = "family-" + UUID.randomUUID().toString().substring(0, 8);
        FamilyMember member = new FamilyMember(familyId, email, fullName, MemberRole.ADMIN);
        familyMemberRepository.save(member);
        return mapMember(member);
    }

    public List<MemberResponse> findMembersByFamily(String familyId) {
        return familyMemberRepository.findByFamilyId(familyId).stream().map(this::mapMember).toList();
    }

    @Transactional
    public InviteResponse createInvite(InviteRequest request) {
        String familyId = request.familyId();
        String adminEmail = normalizeEmail(request.adminEmail());
        Optional<FamilyMember> admin = familyMemberRepository.findByEmail(adminEmail);
        if (admin.isEmpty() || !familyId.equals(admin.get().getFamilyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a family admin can invite members.");
        }
        if (admin.get().getRole() != MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins may create invites.");
        }

        FamilyInvite familyInvite = new FamilyInvite(familyId, adminEmail, OffsetDateTime.now().plusHours(24));
        FamilyInvite saved = familyInviteRepository.save(familyInvite);

        return new InviteResponse(saved.getFamilyId(), saved.getToken(), saved.getCreatedByEmail(), saved.getExpiresAt().toString());
    }

    @Transactional
    public MemberResponse acceptInvite(AcceptInviteRequest request) {
        String email = normalizeEmail(request.email());
        String fullName = request.fullName() == null ? "Family member" : request.fullName().trim();
        if (email.isBlank() || fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and full name are required.");
        }

        FamilyInvite invite = familyInviteRepository.findByToken(request.token())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation was not found."));

        if (!invite.isActive() || invite.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is no longer valid.");
        }

        if (familyMemberRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This member already belongs to a family.");
        }

        FamilyMember member = new FamilyMember(invite.getFamilyId(), email, fullName, MemberRole.MEMBER);
        member = familyMemberRepository.save(member);
        invite.incrementUsedCount();
        if (invite.getUsedCount() >= 1) {
            invite.deactivate();
        }
        familyInviteRepository.save(invite);
        return mapMember(member);
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        String familyId = request.familyId();
        String adminEmail = normalizeEmail(request.adminEmail());
        String name = request.name() == null ? "Family room" : request.name().trim();

        if (familyId == null || familyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Family ID is required.");
        }

        FamilyMember admin = familyMemberRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin account was not found."));
        if (!familyId.equals(admin.getFamilyId()) || admin.getRole() != MemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a family admin can create a room.");
        }

        String roomCode = "room-" + UUID.randomUUID().toString().substring(0, 8);
        MeetingRoom room = new MeetingRoom(familyId, name, roomCode, adminEmail);
        room = meetingRoomRepository.save(room);
        return new RoomResponse(room.getId(), room.getFamilyId(), room.getName(), room.getRoomCode(), room.getStatus().name(), room.getCreatedAt().toString());
    }

    @Transactional
    public TokenResponse createJoinToken(JoinRoomRequest request) {
        String familyId = request.familyId();
        String email = normalizeEmail(request.email());
        FamilyMember member = familyMemberRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found."));

        if (!familyId.equals(member.getFamilyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This member does not belong to the requested family.");
        }

        List<MeetingRoom> rooms = meetingRoomRepository.findByFamilyId(familyId);
        if (rooms.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found.");
        }

        MeetingRoom room = rooms.getFirst();
        if (room.getStatus() == RoomStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This room is closed.");
        }

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(60);
        String payload = String.join("|", room.getRoomCode(), room.getFamilyId(), email, expiresAt.toString());
        String signature = signToken(payload);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString((payload + "." + signature).getBytes(StandardCharsets.UTF_8));
        return new TokenResponse(room.getRoomCode(), room.getFamilyId(), email, token, expiresAt.toString());
    }

    private MemberResponse mapMember(FamilyMember member) {
        return new MemberResponse(member.getFamilyId(), member.getEmail(), member.getFullName(), member.getRole().name(), member.getStatus().name());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String signToken(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign room token.");
        }
    }
}
