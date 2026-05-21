package com.helderruiz.reitera_backend.core.email;

import com.helderruiz.reitera_backend.modules.user.model.User;
import com.helderruiz.reitera_backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    public void sendToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String hashToken = hash(rawToken);

        user.setVerificationToken(hashToken);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        user.setEmailVerified(false);

        userRepository.save(user);

        String link = baseUrl + "/verify?token=" + rawToken;

        emailService.sendEmail(user.getEmail(), "Verify your email", "<a href=\"" + link + "\">Verify email</a>");
    }

    @Transactional
    public void verifyToken(String rawToken) {

        String hashToken = hash(rawToken);

        User user = userRepository.findByVerificationToken(hashToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Expired token");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);

        userRepository.save(user);
    }

    public void resendToken(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEnabled()) {
                sendToken(user);
            }
        });
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
