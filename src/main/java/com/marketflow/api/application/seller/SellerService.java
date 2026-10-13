package com.marketflow.api.application.seller;

import com.marketflow.api.application.common.Actor;
import com.marketflow.api.application.seller.dto.SellerApplicationRequest;
import com.marketflow.api.application.seller.dto.SellerResponse;
import com.marketflow.api.domain.common.exception.ConflictException;
import com.marketflow.api.domain.common.exception.ResourceNotFoundException;
import com.marketflow.api.domain.seller.Seller;
import com.marketflow.api.domain.seller.SellerRepository;
import com.marketflow.api.domain.user.User;
import com.marketflow.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    @Transactional
    public SellerResponse apply(Actor actor, SellerApplicationRequest request) {
        if (sellerRepository.existsByUserId(actor.userId())) {
            throw new ConflictException("SELLER_ALREADY_EXISTS", "User already has a seller profile");
        }
        if (sellerRepository.existsByTaxNumber(request.taxNumber())) {
            throw new ConflictException("TAX_NUMBER_IN_USE", "Tax number is already registered");
        }
        Seller seller = Seller.apply(actor.userId(), request.businessName(), request.legalName(), request.taxNumber(),
                request.email(), request.phone());
        return SellerResponse.from(sellerRepository.save(seller));
    }

    @Transactional
    public SellerResponse approve(UUID sellerId) {
        Seller seller = find(sellerId);
        seller.approve();
        User user = userRepository.findById(seller.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", seller.getUserId()));
        user.promoteToSeller();
        return SellerResponse.from(seller);
    }

    @Transactional
    public SellerResponse reject(UUID sellerId) {
        Seller seller = find(sellerId);
        seller.reject();
        return SellerResponse.from(seller);
    }

    @Transactional(readOnly = true)
    public SellerResponse me(Actor actor) {
        return SellerResponse.from(requireSellerOf(actor));
    }

    @Transactional(readOnly = true)
    public Seller requireSellerOf(Actor actor) {
        return sellerRepository.findByUserId(actor.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile for user", actor.userId()));
    }

    private Seller find(UUID sellerId) {
        return sellerRepository.findById(sellerId).orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId));
    }
}
