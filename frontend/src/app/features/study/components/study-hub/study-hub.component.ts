import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { LucideBookOpen, LucideChevronDown, LucidePlay } from '@lucide/angular';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { CardCountBadgesComponent } from '@shared/components/card-count-badges/card-count-badges.component';
import { DeckResponse } from '@core/models/deck.model';
import { DecksService } from '@features/decks/services/decks.service';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { SessionBackupService } from '@features/study/services/session-backup.service';
import { SessionRecoveryNoticeComponent } from '@features/study/components/session-recovery-notice/session-recovery-notice.component';
import { ToastService } from '@core/toast/toast.service';

const COLLAPSED_STORAGE_KEY = 'study-hub-collapsed';

function readCollapsedState(): Set<number | null> {
  try {
    const stored = localStorage.getItem(COLLAPSED_STORAGE_KEY);

    if (!stored) return new Set();

    return new Set(JSON.parse(stored) as (number | null)[]);
  } catch {
    return new Set();
  }
}

const ACCENT_COLORS = [
  'var(--color-accent-1)',
  'var(--color-accent-2)',
  'var(--color-accent-3)',
  'var(--color-accent-4)',
  'var(--color-accent-5)',
  'var(--color-accent-6)',
] as const;

interface DeckGroup {
  categoryName: string | null;
  categoryId: number | null;
  decks: DeckResponse[];
  totalNew: number;
  totalDue: number;
  totalRelearning: number;
}

@Component({
  selector: 'app-study-hub',
  imports: [
    CardCountBadgesComponent,
    EmptyStateComponent,
    LucideBookOpen,
    LucideChevronDown,
    LucidePlay,
    PageHeaderComponent,
    RouterLink,
    SessionRecoveryNoticeComponent,
    TranslocoPipe,
  ],
  templateUrl: './study-hub.component.html',
})
export class StudyHubComponent implements OnInit {
  private readonly decksService = inject(DecksService);
  private readonly router = inject(Router);
  private readonly sessionBackupService = inject(SessionBackupService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly isLoading = signal(true);
  readonly allDecks = signal<DeckResponse[]>([]);
  readonly collapsedGroups = signal<Set<number | null>>(readCollapsedState());
  readonly pendingBackup = this.sessionBackupService.backup;

  readonly pendingBackupName = computed(() => {
    const backup = this.sessionBackupService.backup();
    return backup?.deckName ?? backup?.categoryName ?? '';
  });

  readonly totalNew = computed(() => this.allDecks().reduce((s, d) => s + d.newCount, 0));
  readonly totalDue = computed(() => this.allDecks().reduce((s, d) => s + d.dueCount, 0));
  readonly totalRelearning = computed(() =>
    this.allDecks().reduce((s, d) => s + d.relearningCount, 0),
  );

  readonly groupedDecks = computed<DeckGroup[]>(() => {
    const map = new Map<number | null, DeckGroup>();

    for (const deck of this.allDecks()) {
      const key = deck.categoryId;

      if (!map.has(key)) {
        map.set(key, {
          categoryName: deck.categoryName,
          categoryId: key,
          decks: [],
          totalNew: 0,
          totalDue: 0,
          totalRelearning: 0,
        });
      }

      const group = map.get(key)!;
      group.decks.push(deck);
      group.totalNew += deck.newCount;
      group.totalDue += deck.dueCount;
      group.totalRelearning += deck.relearningCount;
    }

    return Array.from(map.values()).sort((a, b) => {
      if (a.categoryId === null) return 1;

      if (b.categoryId === null) return -1;

      return (a.categoryName ?? '').localeCompare(b.categoryName ?? '');
    });
  });

  constructor() {
    effect(() => {
      localStorage.setItem(
        COLLAPSED_STORAGE_KEY,
        JSON.stringify(Array.from(this.collapsedGroups())),
      );
    });
  }

  ngOnInit(): void {
    this.loadDecks();
  }

  resumeBackup(): void {
    const backup = this.sessionBackupService.backup();
    if (!backup) return;

    this.sessionBackupService.markAutoResume();

    if (backup.deckId !== null) {
      this.router.navigate(['/study'], {
        queryParams: { deckId: backup.deckId, deckName: backup.deckName },
      });
    } else if (backup.categoryId !== null) {
      this.router.navigate(['/study'], {
        queryParams: { categoryId: backup.categoryId, categoryName: backup.categoryName },
      });
    }
  }

  discardBackup(): void {
    this.sessionBackupService.clear();
  }

  toggleGroup(categoryId: number | null): void {
    this.collapsedGroups.update((set) => {
      const next = new Set(set);

      if (next.has(categoryId)) {
        next.delete(categoryId);
      } else {
        next.add(categoryId);
      }

      return next;
    });
  }

  groupAccentColor(categoryId: number | null): string {
    if (categoryId === null) return 'var(--color-muted)';

    return ACCENT_COLORS[categoryId % ACCENT_COLORS.length];
  }

  private loadDecks(): void {
    this.isLoading.set(true);

    this.decksService.getDecks(0, 100).subscribe({
      next: (page) => {
        this.allDecks.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.toastService.error(this.transloco.translate('common.error'));
      },
    });
  }
}
