package com.helderruiz.reitera_backend.modules.study.service;


import com.helderruiz.reitera_backend.modules.card.model.Card;
import com.helderruiz.reitera_backend.modules.study.model.StudyProgress;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FsrsServiceTest {
    private final FsrsService fsrsService = new FsrsService();

    private User user;
    private Card card;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        card = Card.builder().id(1).build();
        now = LocalDateTime.of(2026, 5, 1, 12, 30);
    }

    @Test
    void schedule_newCard_goodRating_returnsLearningState() {
        StudyProgress result = fsrsService.schedule(null, 3, user, card, now);

        assertThat(result.getState()).isEqualTo(1);
        assertThat(result.getReps()).isEqualTo(1);
        assertThat(result.getLastReview()).isEqualTo(now);
        assertThat(result.getNextReview()).isAfter(now);
    }

    @Test
    void schedule_newCard_forgottenRating_schedulesEarlierThanRemembered() {
        StudyProgress forgottenResult = fsrsService.schedule(null, 1, user, card, now);
        StudyProgress rememberedResult = fsrsService.schedule(null, 3, user, card, now);

        assertThat(forgottenResult.getNextReview()).isBefore(rememberedResult.getNextReview());
    }

    @Test
    void schedule_existingCard_againRating_returnsRelearningAndIncrementLapses() {
        StudyProgress studyProgress = StudyProgress.builder()
                .state(2)
                .stability(5.0)
                .difficulty(5.0)
                .reps(3)
                .lapses(0)
                .lastReview(now.minusDays(1))
                .user(user)
                .card(card)
                .build();

        StudyProgress result = fsrsService.schedule(studyProgress, 1, user, card, now);

        assertThat(result.getState()).isEqualTo(3);
        assertThat(result.getLapses()).isEqualTo(1);
        assertThat(result.getReps()).isEqualTo(4);
        assertThat(result.getLastReview()).isEqualTo(now);
        assertThat(result.getNextReview()).isAfter(now);
        assertThat(result.getStability()).isNotEqualTo(5.0);
        assertThat(result.getDifficulty()).isNotEqualTo(5.0);
    }
}
