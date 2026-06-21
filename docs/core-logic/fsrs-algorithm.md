# FSRS-6 Algorithm — Implementation Reference

Reitera uses the FSRS-6 (Free Spaced Repetition Scheduler) algorithm to compute optimal review intervals for flashcards.

Reference spec: <https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm>

## Core Concepts

Each card tracks three variables per user:

| Variable | Name           | Meaning                                         |
| -------- | -------------- | ----------------------------------------------- |
| **S**    | Stability      | Days until memory drops to 90% retention        |
| **D**    | Difficulty     | How hard the card is for this user (scale 1–10) |
| **R**    | Retrievability | Probability of recall right now (0–1)           |

The user rates each card after reviewing it:

| Rating | Name       | Meaning              |
| ------ | ---------- | -------------------- |
| 1      | Forgotten  | Did not remember     |
| 3      | Remembered | Remembered correctly |

> The FSRS-6 algorithm internally supports ratings 1–4. Reitera uses only 1 and 3 to reduce cognitive friction. Parameters `W[1]`, `W[3]` (initial stability for Hard/Easy), `hardPenalty` (W[15]), and `easyBonus` (W[16]) are inert in the current implementation.

## Card States

```
0 = New        — never studied
1 = Learning   — being learned for the first time
2 = Review     — in long-term spaced repetition rotation
3 = Relearning — was forgotten (Forgotten in Review state), back to learning
```

State transitions (using Reitera's 2-button inputs):

```
New        → [Forgotten / Remembered] → Learning
Learning   → [Remembered]             → Review
Learning   → [Forgotten]              → Learning (stays)
Review     → [Forgotten]              → Relearning
Review     → [Remembered]             → Review (stays)
Relearning → [Remembered]             → Review
Relearning → [Forgotten]              → Relearning (stays)
```

## FSRS-6 Default Parameters (W)

21 values trained on large-scale real review data. Used as-is (no personalisation in this version).

```java
double[] W = {
    0.212,  1.2931, 2.3065, 8.2956,   // w[0-3]:  S₀ per rating 1–4 (w[1] and w[3] inert in Reitera)
    6.4133, 0.8334, 3.0194, 0.001,    // w[4-7]:  D₀ and D update
    1.8722, 0.1666, 0.796,  1.4835,   // w[8-11]: S'_recall factors
    0.0614, 0.2629, 1.6483, 0.6014,   // w[12-15]: S'_forget + Hard penalty (w[15] inert in Reitera)
    1.8729, 0.5425, 0.0912,           // w[16-18]: Easy bonus + same-day scaling (w[16] inert in Reitera)
    0.0658, 0.1542                    // w[19-20]: same-day power + decay
};
```

## Formulas

### Initial stability — `S₀`

Applied on the very first review of a card (state = New).

```
S₀(rating) = W[rating - 1]
```

Examples: Forgotten (1) → 0.212 days · Remembered (3) → 2.307 days · Easy/4 → 8.296 days (inert in Reitera)

### Initial difficulty — `D₀`

```
D₀(rating) = W[4] - exp(W[5] × (rating - 1)) + 1
```

Clamped to [1, 10]. High ratings yield lower difficulty.

### Forgetting curve — `R(t, S)`

Probability of recall after `t` days given stability `S`:

```
factor = 0.9^(1/decay) - 1     where decay = -W[20] = -0.1542
R(t, S) = (1 + factor × t/S)^(-W[20])
```

Guarantees `R(S, S) = 0.9` — when `t = S`, recall probability is exactly 90%.

### Next review interval

```
interval_days = S / factor × (0.9^(1/decay) - 1)   ≈ S days
interval_minutes = interval_days × 1440
```

Intervals are stored and applied at **minute precision**. This allows sub-day intervals for low-stability cards (e.g. Forgotten on a new card → ~305 min).

### Difficulty update — `D'`

Applied on every review after the first:

```
D'(D, rating) = W[7] × D₀(4) + (1 - W[7]) × (D - W[6] × (rating - 3))
```

Mean-reversion: difficulty drifts towards the global average (D₀ at rating=4 ≈ 4.93). Clamped to [1, 10].

### Stability after recall — `S'_recall`

Applied when rating = 3 (Remembered):

```
S'_recall = S × exp(W[8]) × (11 - D) × S^(-W[9])
          × (exp(W[10] × (1 - R)) - 1)
          × hardPenalty   [× W[15] = 0.6014 if rating = 2 — inert in Reitera]
          × easyBonus     [× W[16] = 1.8729 if rating = 4 — inert in Reitera]
```

Stability always increases after a successful recall. A floor of `S + 0.01` is applied.

### Stability after forgetting — `S'_forget`

Applied when rating = 1 (Forgotten):

```
S'_forget = W[11] × D^(-W[12]) × ((S + 1)^W[13] - 1) × exp(W[14] × (1 - R))
```

The card returns to a low stability but retains some residual memory — it is not as hard as the very first review.

## Implementation Notes

- **`FsrsService`** — pure Java, no Spring dependencies, fully unit-testable.
- **`StudyService`** — orchestrates DB access, calls `FsrsService`, persists `StudyProgress` and `ReviewLog` in a single `@Transactional` batch.
- **Minimum interval** — 1 minute (never schedules a review for the exact current moment).
- **`scheduledDays`** field — stores whole days for analytics; sub-day cards store `0`.
- **`nextReview`** — `LocalDateTime` with minute precision; this is the authoritative field for scheduling.
- **Elapsed time** — computed in minutes and converted to decimal days for accurate retrievability on sub-day reviews.
