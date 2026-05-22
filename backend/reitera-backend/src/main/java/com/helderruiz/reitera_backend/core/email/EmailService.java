package com.helderruiz.reitera_backend.core.email;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final Resend resend;
    private final String from;

    public EmailService(
            @Value("${api.security.resend.api-key}")
            String resendKey,

            @Value("${app.email.from:Acme <onboarding@resend.dev>}")
            String from
    ) {

        this.resend = new Resend(resendKey);
        this.from = from;
    }

    @Async
    public void sendEmail(String to, String subject, String html) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);

            log.info("Email sent, id: {}", response.getId());

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}