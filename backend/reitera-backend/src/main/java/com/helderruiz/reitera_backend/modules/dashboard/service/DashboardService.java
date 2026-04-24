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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DeckRepository deckRepository;
    private final StudyProgressRepository studyProgressRepository;
    private final CardRepository cardRepository;
    private final ReviewLogRepository reviewLogRepository;

    public DashboardStatsDTO getStats(User user) {
        UUID userId = user.getId();

        List<Integer> deckIds = deckRepository.findAllIdsByOwnerId(userId);

        if (deckIds.isEmpty()) {
            return new DashboardStatsDTO(0, 0L, 0L);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        long dueCount = studyProgressRepository.countDueByUserAndDeckIds(userId, deckIds, now)
                .stream()
                .mapToLong(row -> ((Number) row[2]).longValue())
                .sum();

        long newCount = cardRepository.countNewCardsByDeckIdsAndUser(deckIds, userId)
                .stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        long totalDueToday = dueCount + newCount;

        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        long studiedToday = reviewLogRepository.countDistinctCardsReviewedBetween(userId, startOfToday, startOfTomorrow);

        LocalDateTime streakSince = today.minusDays(365).atStartOfDay();
        List<LocalDate> reviewDates = reviewLogRepository.findDistinctReviewDatesSince(userId, streakSince);
        int streak = computeStreak(reviewDates, today);

        return new DashboardStatsDTO(streak, totalDueToday, studiedToday);
    }

    /**
     * Returns per-day distinct card review counts for the 365 days ending today.
     * Only days on which at least one card was reviewed are included; the caller
     * is responsible for treating absent dates as zero.
     *
     * @param user the authenticated user
     * @return list of daily counts ordered by date ascending
     */
    public List<DailyStudyCountDTO> getHeatmap(User user) {
        LocalDateTime since = LocalDate.now().minusDays(364).atStartOfDay();
        List<Object[]> rows = reviewLogRepository.countDistinctCardsPerDaySince(user.getId(), since);
        return rows.stream()
                .map(row -> new DailyStudyCountDTO(row[0].toString(), ((Number) row[1]).longValue()))
                .toList();
    }

    /**
     * Returns the last {@code limit} decks the user has studied, enriched with
     * due/new/relearning card counts. Order matches the review log (most recent first).
     *
     * @param user  the authenticated user
     * @param limit maximum number of decks to return (e.g. 5 for the dashboard widget)
     */
    public List<LastStudiedDeckDTO> getLastStudied(User user, int limit) {
        UUID userId = user.getId();

        List<Object[]> rows = reviewLogRepository.findLastStudiedDeckIds(userId, limit);
        if (rows.isEmpty()) return List.of();

        List<Integer> orderedDeckIds = rows.stream()
                .map(row -> ((Number) row[0]).intValue())
                .toList();

        Map<Integer, String> lastStudiedMap = new HashMap<>();
        for (Object[] row : rows) {
            int deckId = ((Number) row[0]).intValue();
            lastStudiedMap.put(deckId, row[1].toString());
        }

        Map<Integer, Deck> deckMap = deckRepository.findAllById(orderedDeckIds)
                .stream()
                .collect(Collectors.toMap(Deck::getId, d -> d));

        LocalDateTime now = LocalDateTime.now();

        Map<Integer, Long> dueCountMap = new HashMap<>();
        Map<Integer, Long> relearningCountMap = new HashMap<>();
        studyProgressRepository.countDueByUserAndDeckIds(userId, orderedDeckIds, now)
                .forEach(row -> {
                    int deckId = ((Number) row[0]).intValue();
                    int state = ((Number) row[1]).intValue();
                    long count = ((Number) row[2]).longValue();
                    if (state == 3) {
                        relearningCountMap.merge(deckId, count, Long::sum);
                    } else {
                        dueCountMap.merge(deckId, count, Long::sum);
                    }
                });

        Map<Integer, Long> newCountMap = new HashMap<>();
        cardRepository.countNewCardsByDeckIdsAndUser(orderedDeckIds, userId)
                .forEach(row -> newCountMap.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue()));

        return orderedDeckIds.stream()
                .filter(deckMap::containsKey)
                .map(deckId -> {
                    Deck deck = deckMap.get(deckId);
                    return new LastStudiedDeckDTO(
                            deckId,
                            deck.getTitle(),
                            deck.getCategory() != null ? deck.getCategory().getName() : null,
                            newCountMap.getOrDefault(deckId, 0L),
                            dueCountMap.getOrDefault(deckId, 0L),
                            relearningCountMap.getOrDefault(deckId, 0L),
                            lastStudiedMap.get(deckId)
                    );
                })
                .toList();
    }

    /**
     * Computes the current study streak from a descending list of distinct review dates.
     * A streak is the number of consecutive days ending today or yesterday on which
     * the user reviewed at least one card. If the user studied neither today nor yesterday
     * the streak is 0.
     */
    private int computeStreak(List<LocalDate> dates, LocalDate today) {
        if (dates.isEmpty()) return 0;

        LocalDate expected = dates.getFirst().equals(today) ? today : today.minusDays(1);

        if (!dates.getFirst().equals(expected)) return 0;

        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
