package com.helderruiz.reitera_backend.modules.auth.filter;

import com.helderruiz.reitera_backend.modules.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Intercepts HTTP requests to validate JWTs present in the Authorization header.
     * Populates the SecurityContext if a valid token is provided.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Check for the presence and correct formatting of the Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Delegate to the next filter in the chain (e.g., for public endpoints)
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the JWT payload string (removing the "Bearer " prefix)
        jwt = authHeader.substring(7);

        // 3. Extract the subject (email) using the JwtService.
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (JwtException e) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Proceed with authentication if the subject exists and the context is currently unauthenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Retrieve the user entity from the persistent store
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 5. Cryptographically validate the token and verify its expiration
            if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {

                // 6. Instantiate the authentication token containing principal, credentials (null), and authorities
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Attach HTTP request details (e.g., IP address, session ID) to the authentication object
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Inject the authenticated token into the ThreadLocal SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Continue the execution of the standard filter chain
        filterChain.doFilter(request, response);
    }
}