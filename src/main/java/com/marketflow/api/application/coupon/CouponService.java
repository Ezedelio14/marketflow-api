package com.marketflow.api.application.coupon;

import com.marketflow.api.application.common.PageResponse;
import com.marketflow.api.application.coupon.dto.CouponResponse;
import com.marketflow.api.application.coupon.dto.CreateCouponRequest;
import com.marketflow.api.domain.common.exception.ConflictException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.domain.coupon.Coupon;
import com.marketflow.api.domain.coupon.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository repository;

    @Transactional
    public CouponResponse create(CreateCouponRequest r) {
        if (repository.existsByCodeIgnoreCase(r.code())) {
            throw new ConflictException("COUPON_CODE_IN_USE", "Coupon code already exists");
        }
        Coupon coupon = Coupon.create(r.code(), r.type(), r.value(), r.minimumAmount(), r.maximumDiscount(),
                r.usageLimit(), r.startsAt(), r.expiresAt());
        return CouponResponse.from(repository.save(coupon));
    }

    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> list(Pageable pageable) {
        return PageResponse.from(repository.findAll(pageable), CouponResponse::from);
    }

    @Transactional
    public CouponResponse deactivate(UUID id) {
        Coupon coupon = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Coupon", id));
        coupon.deactivate();
        return CouponResponse.from(coupon);
    }
}
