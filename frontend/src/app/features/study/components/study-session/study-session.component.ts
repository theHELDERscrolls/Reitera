import {
  Component,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import {
  LucideArrowLeft,
  LucideBookCheck,
  LucideBookOpen,
  LucideCircleCheck,
} from '@lucide/angular';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { CardRating, DueCard, StudySessionRequest } from '@core/models/study.model';
import { StudyCardComponent } from '../study-card/study-card.component';
import { StudyService } from '@features/study/services/study.service';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';

type SessionState = 'loading' | 'empty' | 'active' | 'complete';

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
    StudyCardComponent,
    TranslocoPipe,
  ],
  templateUrl: './study-session.component.html',
})
export class StudySessionComponent implements OnInit {
  readonly deckId = input<number | null>(null);
  readonly categoryId = input<number | null>(null);

  private readonly studyService = inject(StudyService);
  private readonly router = inject(Router);

  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly isComplete = signal(false);
  readonly showBackDialog = signal(false);

  readonly queue = signal<DueCard[]>([]);
  readonly originalTotal = signal(0);
  readonly ratings = signal<Map<number, number>>(new Map());
  readonly againCount = signal<Map<number, number>>(new Map());
  readonly revealed = signal(false);

  readonly currentCard = computed(() => this.queue()[0] ?? null);

  readonly sessionState = computed<SessionState>(() => {
    if (this.isLoading()) return 'loading';

    if (this.originalTotal() === 0) return 'empty';

    if (this.isComplete()) return 'complete';

    return 'active';
  });

  readonly progress = computed(() => {
    const total = this.originalTotal();
    const reviewed = this.ratings().size;

    return {
      reviewed,
      total,
      percent: total > 0 ? Math.round((reviewed / total) * 100) : 0,
    };
  });

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

  revealAnswer(): void {
    this.revealed.set(true);
  }

  rate(rating: 1 | 2 | 3 | 4): void {
    const card = this.currentCard();

    if (!card) return;

    this.ratings.update((m) => new Map(m).set(card.id, rating));

    if (rating === 1) {
      const count = (this.againCount().get(card.id) ?? 0) + 1;

      this.againCount.update((m) => new Map(m).set(card.id, count));

      this.queue.update((q) => {
        const [, ...rest] = q;

        if (count < 3) {
          const pos = Math.min(10, rest.length);

          rest.splice(pos, 0, card);
        }

        return rest;
      });
    } else {
      this.queue.update(([, ...rest]) => rest);
    }

    this.revealed.set(false);
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

  private submitSession(navigateToHub: boolean): void {
    const ratingsMap = this.ratings();

    if (ratingsMap.size === 0) {
      if (navigateToHub) this.router.navigate(['/study']);

      return;
    }

    this.isSubmitting.set(true);

    const ratings: CardRating[] = Array.from(ratingsMap.entries()).map(([cardId, rating]) => ({
      cardId,
      rating: rating as 1 | 2 | 3 | 4,
    }));

    const request: StudySessionRequest = {
      deckId: this.deckId() ?? null,
      categoryId: this.categoryId() ?? null,
      ratings,
    };

    this.studyService.processSession(request).subscribe({
      next: () => {
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
