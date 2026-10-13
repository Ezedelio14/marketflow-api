package com.marketflow.api.domain.user;

import com.marketflow.api.domain.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends AuditableEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    public static User register(String email, String passwordHash, UserRole role) {
        User user = new User();
        user.email = email.toLowerCase().trim();
        user.passwordHash = passwordHash;
        user.role = role;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void promoteToSeller() {
        if (role == UserRole.CUSTOMER) {
            role = UserRole.SELLER;
        }
    }
}
