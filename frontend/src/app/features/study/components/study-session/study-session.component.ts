import {
  Component,
  OnDestroy,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { environment } from '@environments/environment';
import {
  LucideArrowLeft,
  LucideBookCheck,
  LucideBookOpen,
  LucideCircleCheck,
} from '@lucide/angular';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { CardRating, DueCard, SessionBackup, StudySessionRequest } from '@core/models/study.model';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { SessionBackupService } from '@features/study/services/session-backup.service';
import { SessionRecoveryNoticeComponent } from '@features/study/components/session-recovery-notice/session-recovery-notice.component';
import { StudyCardComponent } from '../study-card/study-card.component';
import { StudyService } from '@features/study/services/study.service';
import { StudyStateService } from '@features/study/services/study-state.service';
import { ToastService } from '@core/toast/toast.service';

type SessionState = 'loading' | 'recovery' | 'empty' | 'active' | 'complete';

@Component({
  selector: 'app-study-session',
  imports: [
    ConfirmDialogComponent,
    EmptyStateComponent,
    LucideArrowLeft,
    LucideBookCheck,
    LucideBookOpen,
    LucideCircleCheck,
    RouterLink,
    SessionRecoveryNoticeComponent,
    StudyCardComponent,
    TranslocoPipe,
  ],
  templateUrl: './study-session.component.html',
})
export class StudySessionComponent implements OnInit, OnDestroy {
  readonly categoryId = input<number | null>(null);
  readonly categoryName = input<string | null>(null);
  readonly deckId = input<number | null>(null);
  readonly deckName = input<string | null>(null);

  private readonly router = inject(Router);
  private readonly sessionBackupService = inject(SessionBackupService);
  private readonly studyService = inject(StudyService);
  private readonly studyState = inject(StudyStateService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly isComplete = signal(false);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly recoveryBackup = signal<SessionBackup | null>(null);
  readonly showBackDialog = signal(false);

  readonly originalTotal = signal(0);
  readonly queue = signal<DueCard[]>([]);
  readonly ratings = signal<Map<number, 1 | 3>>(new Map());
  readonly revealed = signal(false);

  private startedAt = 0;

  readonly currentCard = computed(() => this.queue()[0] ?? null);

  readonly sessionState = computed<SessionState>(() => {
    if (this.isLoading()) return 'loading';

    if (this.recoveryBackup() !== null) return 'recovery';

    if (this.originalTotal() === 0) return 'empty';

    if (this.isComplete()) return 'complete';

    return 'active';
  });

  readonly progress = computed(() => {
    const total = this.originalTotal();
    const remembered = [...this.ratings().values()].filter((rating) => rating === 3).length;

    return {
      reviewed: remembered,
      total,
      percent: total > 0 ? Math.round((remembered / total) * 100) : 0,
    };
  });

  readonly deactivationRequested = this.studyState.deactivationRequested;

  readonly recoveryName = computed(() => {
    const backup = this.recoveryBackup();

    if (!backup) return '';

    return backup.deckName ?? backup.categoryName ?? '';
  });

  private readonly handleBeforeUnload = (event: BeforeUnloadEvent): void => {
    if (this.ratings().size === 0) return;

    event.preventDefault();

    const ratings: CardRating[] = Array.from(this.ratings().entries()).map(([cardId, rating]) => ({
      cardId,
      rating,
    }));

    const request: StudySessionRequest = {
      deckId: this.deckId() ?? null,
      categoryId: this.categoryId() ?? null,
      ratings,
    };

    navigator.sendBeacon(
      `${environment.apiUrl}/study/sessions`,
      new Blob([JSON.stringify(request)], { type: 'application/json' }),
    );
  };

  constructor() {
    effect(() => {
      const queueEmpty = this.queue().length === 0;
      const hasStarted = this.originalTotal() > 0;
      const idle = !this.isLoading() && !this.isSubmitting() && !this.isComplete();

      if (queueEmpty && hasStarted && idle) {
        untracked(() => this.submitSession(false));
      }
    });
  }

  ngOnInit(): void {
    this.startedAt = Date.now();

    const backup = this.sessionBackupService.load();

    const backupMatchesSession =
      backup !== null &&
      backup.deckId === (this.deckId() ?? null) &&
      backup.categoryId === (this.categoryId() ?? null);

    if (backupMatchesSession) {
      if (this.sessionBackupService.consumeAutoResume()) {
        this.isLoading.set(false);
        this.resumeBackup();
      } else {
        this.recoveryBackup.set(backup);
        this.isLoading.set(false);
      }
    } else {
      this.loadDueCards();
    }

    window.addEventListener('beforeunload', this.handleBeforeUnload);
  }

  ngOnDestroy(): void {
    window.removeEventListener('beforeunload', this.handleBeforeUnload);
    this.studyState.hasPendingRatings.set(false);
  }

  revealAnswer(): void {
    this.revealed.set(true);
  }

  rate(rating: 1 | 3): void {
    const card = this.currentCard();

    if (!card) return;

    this.ratings.update((currentRatings) => new Map(currentRatings).set(card.id, rating));

    if (rating === 1) {
      this.queue.update((currentQueue) => {
        const [, ...remainingCards] = currentQueue;
        remainingCards.push(card);
        return remainingCards;
      });
    } else {
      this.queue.update(([, ...remainingCards]) => remainingCards);
    }

    this.revealed.set(false);
    this.studyState.hasPendingRatings.set(true);

    const ratingsArray: CardRating[] = Array.from(this.ratings().entries()).map(
      ([cardId, cardRating]) => ({ cardId, rating: cardRating }),
    );

    this.sessionBackupService.save({
      version: '1',
      deckId: this.deckId() ?? null,
      deckName: this.deckName() ?? null,
      categoryId: this.categoryId() ?? null,
      categoryName: this.categoryName() ?? null,
      ratings: ratingsArray,
      startedAt: this.startedAt,
      updatedAt: Date.now(),
    });
  }

  goBack(): void {
    this.showBackDialog.set(true);
  }

  confirmGoBack(): void {
    this.showBackDialog.set(false);
    this.router.navigate(['/study']);
  }

  endSession(): void {
    if (this.isSubmitting() || this.isComplete()) return;

    this.submitSession(true);
  }

  resumeBackup(): void {
    const backup = this.recoveryBackup();

    if (!backup) return;

    this.isSubmitting.set(true);

    const request: StudySessionRequest = {
      deckId: backup.deckId,
      categoryId: backup.categoryId,
      ratings: backup.ratings,
    };

    this.studyService.processSession(request).subscribe({
      next: (response) => {
        this.sessionBackupService.clear();
        this.recoveryBackup.set(null);
        this.isSubmitting.set(false);

        if (response.cardsReviewed === 0) {
          this.toastService.info(this.transloco.translate('study.session.recovery.noCards'));
        } else {
          this.toastService.success(this.transloco.translate('study.session.recovery.success'));
        }

        this.loadDueCards();
      },
      error: () => {
        this.isSubmitting.set(false);
        this.toastService.error(this.transloco.translate('common.error'));
      },
    });
  }

  discardBackup(): void {
    this.sessionBackupService.clear();
    this.recoveryBackup.set(null);
    this.loadDueCards();
  }

  confirmDeactivation(): void {
    this.studyState.confirmDeactivation();
  }

  cancelDeactivation(): void {
    this.studyState.cancelDeactivation();
  }

  private loadDueCards(): void {
    this.isLoading.set(true);

    const deckId = this.deckId() ?? undefined;
    const categoryId = this.categoryId() ?? undefined;

    this.studyService.getDueCards(deckId, categoryId).subscribe({
      next: (cards) => {
        this.queue.set([...cards]);
        this.originalTotal.set(cards.length);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  private submitSession(navigateToHub: boolean): void {
    const ratingsMap = this.ratings();

    if (ratingsMap.size === 0) {
      if (navigateToHub) this.router.navigate(['/study']);

      return;
    }

    this.isSubmitting.set(true);

    const ratings: CardRating[] = Array.from(ratingsMap.entries()).map(([cardId, rating]) => ({
      cardId,
      rating,
    }));

    const request: StudySessionRequest = {
      deckId: this.deckId() ?? null,
      categoryId: this.categoryId() ?? null,
      ratings,
    };

    this.studyService.processSession(request).subscribe({
      complete: () => {
        this.sessionBackupService.clear();
        this.studyState.hasPendingRatings.set(false);
        this.isSubmitting.set(false);

        if (navigateToHub) {
          this.router.navigate(['/study']);
        } else {
          this.isComplete.set(true);
        }
      },
      error: () => {
        this.isSubmitting.set(false);
      },
    });
  }
}
