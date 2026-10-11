package com.familyconnect.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_invites")
public class FamilyInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private String familyId;

    @Column(name = "created_by_email", nullable = false)
    private String createdByEmail;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    protected FamilyInvite() {
    }

    public FamilyInvite(String familyId, String createdByEmail, OffsetDateTime expiresAt) {
        this.familyId = familyId;
        this.createdByEmail = createdByEmail;
        this.token = UUID.randomUUID().toString();
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getFamilyId() {
        return familyId;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public String getToken() {
        return token;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void incrementUsedCount() {
        this.usedCount += 1;
    }
}
