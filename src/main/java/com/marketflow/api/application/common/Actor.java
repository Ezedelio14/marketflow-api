package com.marketflow.api.application.common;

import com.marketflow.api.domain.user.UserRole;

import java.util.UUID;

/**
 * Who is executing a use case. Built by the presentation layer from the authenticated JWT principal.
 */
public record Actor(UUID userId, String email, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isSeller() {
        return role == UserRole.SELLER;
    }

    public String label() {
        return role.name() + ":" + userId;
    }
}
