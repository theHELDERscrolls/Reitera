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

        emailService.sendEmail(user.getEmail(), "Verify your Reitera account", buildVerificationEmail(user.getFirstName(), link));
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

    private String buildVerificationEmail(String firstName, String link) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Verify your Reitera account</title>
                </head>
                <body style="margin:0;padding:0;background-color:#eff1f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#eff1f5;padding:48px 16px;">
                    <tr>
                      <td align="center">
                        <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;">

                          <!-- Wordmark -->
                          <tr>
                            <td align="center" style="padding-bottom:20px;">
                              <span style="font-size:28px;font-weight:700;color:#8839ef;letter-spacing:-0.5px;">Reitera</span>
                            </td>
                          </tr>

                          <!-- Card -->
                          <tr>
                            <td style="background-color:#ffffff;border-radius:16px;padding:40px;border:1px solid #bcc0cc;">

                              <!-- Heading -->
                              <p style="margin:0 0 8px;font-size:20px;font-weight:700;color:#4c4f69;letter-spacing:-0.3px;">Verify your email address</p>

                              <!-- Body -->
                              <p style="margin:0 0 32px;font-size:15px;color:#6c6f85;line-height:1.7;">
                                Hi <strong style="color:#4c4f69;">%s</strong>, thanks for signing up to Reitera.<br/>
                                Click the button below to confirm your email and activate your account.<br/>
                                <span style="font-size:13px;color:#8c8fa1;">This link expires in 24 hours.</span>
                              </p>

                              <!-- CTA button — centered -->
                              <table cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td align="center" style="padding-bottom:32px;">
                                    <table cellpadding="0" cellspacing="0">
                                      <tr>
                                        <td align="center" style="background-color:#8839ef;border-radius:10px;">
                                          <a href="%s"
                                             style="display:inline-block;padding:14px 36px;font-size:15px;font-weight:600;color:#eff1f5;text-decoration:none;border-radius:10px;letter-spacing:0.1px;">
                                            Verify email address
                                          </a>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>

                              <!-- Divider -->
                              <table cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td style="border-top:1px solid #e9eaf0;padding-top:24px;">
                                    <p style="margin:0 0 6px;font-size:12px;color:#8c8fa1;">Button not working? Paste this link in your browser:</p>
                                    <p style="margin:0;font-size:12px;color:#8839ef;word-break:break-all;">%s</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td align="center" style="padding-top:20px;">
                              <p style="margin:0;font-size:12px;color:#8c8fa1;line-height:1.6;">
                                If you didn't create a Reitera account, you can safely ignore this email.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, link, link);
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
