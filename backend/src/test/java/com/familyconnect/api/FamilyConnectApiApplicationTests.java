package com.familyconnect.api;

import com.familyconnect.api.dto.AcceptInviteRequest;
import com.familyconnect.api.dto.AdminRegistrationRequest;
import com.familyconnect.api.dto.CreateRoomRequest;
import com.familyconnect.api.dto.InviteRequest;
import com.familyconnect.api.dto.JoinRoomRequest;
import com.familyconnect.api.dto.MemberResponse;
import com.familyconnect.api.dto.RoomResponse;
import com.familyconnect.api.dto.TokenResponse;
import com.familyconnect.api.service.FamilyAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FamilyConnectApiApplicationTests {

    @Autowired
    private FamilyAuthService familyAuthService;

    @Test
    void familyFlowWorksEndToEnd() {
        MemberResponse admin = familyAuthService.createFamilyAdmin(new AdminRegistrationRequest("ava@example.com", "Ava Patel"));
        assertEquals("ava@example.com", admin.email());
        assertNotNull(admin.familyId());

        var invite = familyAuthService.createInvite(new InviteRequest(admin.familyId(), "ava@example.com"));
        assertEquals(admin.familyId(), invite.familyId());

        MemberResponse acceptedMember = familyAuthService.acceptInvite(new AcceptInviteRequest(invite.token(), "luca@example.com", "Luca Patel"));
        assertEquals("luca@example.com", acceptedMember.email());
        assertEquals(admin.familyId(), acceptedMember.familyId());

        RoomResponse room = familyAuthService.createRoom(new CreateRoomRequest(admin.familyId(), "Saturday call", "ava@example.com"));
        assertEquals("Saturday call", room.name());

        List<MemberResponse> members = familyAuthService.findMembersByFamily(admin.familyId());
        assertTrue(members.size() >= 2);

        TokenResponse token = familyAuthService.createJoinToken(new JoinRoomRequest(admin.familyId(), "luca@example.com"));
        assertEquals("luca@example.com", token.email());
        assertNotNull(token.token());
    }
}
