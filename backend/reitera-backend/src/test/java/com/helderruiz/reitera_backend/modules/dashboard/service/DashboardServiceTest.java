package com.helderruiz.reitera_backend.modules.dashboard.service;

import com.helderruiz.reitera_backend.modules.card.repository.CardRepository;
import com.helderruiz.reitera_backend.modules.dashboard.dto.DailyStudyCountDTO;
import com.helderruiz.reitera_backend.modules.dashboard.dto.DashboardStatsDTO;
import com.helderruiz.reitera_backend.modules.dashboard.dto.LastStudiedDeckDTO;
import com.helderruiz.reitera_backend.modules.deck.model.Deck;
import com.helderruiz.reitera_backend.modules.deck.repository.DeckRepository;
import com.helderruiz.reitera_backend.modules.study.repository.ReviewLogRepository;
import com.helderruiz.reitera_backend.modules.study.repository.StudyProgressRepository;
import com.helderruiz.reitera_backend.modules.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    private User mockUser;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private StudyProgressRepository studyProgressRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ReviewLogRepository reviewLogRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void getStats_returnsZeros_whenNoDeckExists() {
        when(deckRepository.findAllIdsByOwnerId(mockUser.getId())).thenReturn(List.of());

        DashboardStatsDTO result = dashboardService.getStats(mockUser);

        assertThat(result.streak()).isEqualTo(0);
        assertThat(result.totalDueToday()).isEqualTo(0L);
        assertThat(result.studiedToday()).isEqualTo(0L);
    }

    @Test
    void getStats_returnsCorrectAggregate() {
        List<Object[]> progressRows = Collections.singletonList(new Object[]{1, 1, 2L});
        List<Object[]> newCardRows = Collections.singletonList(new Object[]{1, 3L});

        when(deckRepository.findAllIdsByOwnerId(mockUser.getId())).thenReturn(List.of(1));
        when(studyProgressRepository.countDueByUserAndDeckIds(any(), anyList(), any()))
                .thenReturn(progressRows);
        when(cardRepository.countNewCardsByDeckIdsAndUser(anyList(), any()))
                .thenReturn(newCardRows);
        when(reviewLogRepository.countDistinctCardsReviewedBetween(any(), any(), any())).thenReturn(5L);
        when(reviewLogRepository.findDistinctReviewDatesSince(any(), any()))
                .thenReturn(List.of(LocalDate.now()));

        DashboardStatsDTO result = dashboardService.getStats(mockUser);

        assertThat(result.streak()).isEqualTo(1);
        assertThat(result.totalDueToday()).isEqualTo(5L);
        assertThat(result.studiedToday()).isEqualTo(5L);
    }

    @Test
    void getHeatmap_returnsDailyList() {
        List<Object[]> heatmapRows = Collections.singletonList(new Object[]{"2026-05-13", 5L});
        when(reviewLogRepository.countDistinctCardsPerDaySince(any(), any()))
                .thenReturn(heatmapRows);

        List<DailyStudyCountDTO> result = dashboardService.getHeatmap(mockUser);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).date()).isEqualTo("2026-05-13");
        assertThat(result.get(0).count()).isEqualTo(5L);
    }

    @Test
    void getLastStudied_returnsLimitedList() {
        List<Object[]> lastStudiedRows = Collections.singletonList(new Object[]{1, "2026-05-13T10:00"});
        List<Object[]> emptyRows = Collections.emptyList();

        Deck deck = Deck.builder().id(1).title("Java Basics").build();
        when(reviewLogRepository.findLastStudiedDeckIds(eq(mockUser.getId()), eq(3)))
                .thenReturn(lastStudiedRows);
        when(deckRepository.findAllById(anyList())).thenReturn(List.of(deck));
        when(studyProgressRepository.countDueByUserAndDeckIds(any(), anyList(), any()))
                .thenReturn(emptyRows);
        when(cardRepository.countNewCardsByDeckIdsAndUser(anyList(), any()))
                .thenReturn(emptyRows);

        List<LastStudiedDeckDTO> result = dashboardService.getLastStudied(mockUser, 3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deckId()).isEqualTo(1);
        assertThat(result.get(0).name()).isEqualTo("Java Basics");
        assertThat(result.get(0).newCount()).isEqualTo(0L);
    }
}
