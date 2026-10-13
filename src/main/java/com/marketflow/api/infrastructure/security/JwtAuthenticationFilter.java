package com.marketflow.api.infrastructure.security;

import com.marketflow.api.application.auth.TokenRevocationStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless Bearer authentication: validates the JWT and populates the SecurityContext. Invalid or revoked tokens
 * simply leave the request anonymous; the entry point then answers 401 for protected routes.
 *
 * <p>Not a Spring bean on purpose, so it is only registered inside the security filter chain.</p>
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenRevocationStore revocationStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER) && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenProvider.parse(header.substring(BEARER.length()).trim())
                    .filter(user -> !revocationStore.isRevoked(user.tokenId()))
                    .ifPresent(user -> {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        chain.doFilter(request, response);
    }
}
