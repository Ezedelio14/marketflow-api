package com.marketflow.api.domain.seller;

import com.marketflow.api.domain.common.BaseEntity;
import com.marketflow.api.domain.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "sellers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seller extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "tax_number", nullable = false, unique = true, length = 32)
    private String taxNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SellerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Seller apply(UUID userId, String businessName, String legalName, String taxNumber,
                               String email, String phone) {
        Seller seller = new Seller();
        seller.userId = userId;
        seller.businessName = businessName;
        seller.legalName = legalName;
        seller.taxNumber = taxNumber;
        seller.email = email;
        seller.phone = phone;
        seller.status = SellerStatus.INACTIVE;
        seller.verificationStatus = VerificationStatus.PENDING;
        return seller;
    }

    public void approve() {
        verificationStatus = VerificationStatus.APPROVED;
        status = SellerStatus.ACTIVE;
    }

    public void reject() {
        verificationStatus = VerificationStatus.REJECTED;
        status = SellerStatus.INACTIVE;
    }

    public boolean canSell() {
        return status == SellerStatus.ACTIVE && verificationStatus == VerificationStatus.APPROVED;
    }

    public void ensureCanSell() {
        if (!canSell()) {
            throw new BusinessRuleException("SELLER_NOT_APPROVED", "Seller account is not approved to sell");
        }
    }
}
