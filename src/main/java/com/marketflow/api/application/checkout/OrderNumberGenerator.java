package com.marketflow.api.application.checkout;

import com.marketflow.api.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Generates numbers like {@code MF-261013-7Q2K9XPA} (date + 8 random base-32 chars). */
@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final SecureRandom random = new SecureRandom();
    private final OrderRepository orderRepository;
    private final Clock clock;

    public String next() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder("MF-").append(LocalDate.now(clock).format(DATE)).append('-');
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            candidate = sb.toString();
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }
}
