package com.marketflow.api.infrastructure.bootstrap;

import com.marketflow.api.domain.user.User;
import com.marketflow.api.domain.user.UserRepository;
import com.marketflow.api.domain.user.UserRole;
import com.marketflow.api.config.MarketflowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the first administrator account if it does not exist yet. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MarketflowProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        MarketflowProperties.Bootstrap cfg = properties.bootstrap();
        if (cfg == null || cfg.adminEmail() == null || userRepository.existsByEmailIgnoreCase(cfg.adminEmail())) {
            return;
        }
        userRepository.save(User.register(cfg.adminEmail(), passwordEncoder.encode(cfg.adminPassword()), UserRole.ADMIN));
        log.info("Bootstrap admin account created: {}", cfg.adminEmail());
    }
}
