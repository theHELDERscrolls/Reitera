import { LucideBookOpen, LucidePlus } from '@lucide/angular';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { CardFormComponent } from '../card-form/card-form.component';
import { CardResponse } from '@core/models/card.model';
import { CardsTableComponent } from '../cards-table/cards-table.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { VisibilityBadgeComponent } from '../visibility-badge/visibility-badge.component';
import { DeckDetailService } from '../../services/deck-detail.service';
import { DeckResponse, DeckStats } from '@core/models/deck.model';
import { Tag } from '@core/models/tag.model';
import { ToastService } from '@core/toast/toast.service';
import PaginationComponent from '@shared/components/pagination/pagination.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-deck-detail',
  imports: [
    CardFormComponent,
    CardsTableComponent,
    ConfirmDialogComponent,
    DatePipe,
    LucideBookOpen,
    LucidePlus,
    PaginationComponent,
    RouterLink,
    TranslocoPipe,
    VisibilityBadgeComponent,
  ],
  templateUrl: './deck-detail.component.html',
})
export default class DeckDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(DeckDetailService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly deckId = Number(this.route.snapshot.paramMap.get('id'));

  readonly deck = signal<DeckResponse | null>(null);
  readonly stats = signal<DeckStats | null>(null);
  readonly cards = signal<CardResponse[]>([]);
  readonly tags = signal<Tag[]>([]);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly isLoading = signal(true);
  readonly isCardFormOpen = signal(false);
  readonly cardToEdit = signal<CardResponse | null>(null);
  readonly cardToDelete = signal<CardResponse | null>(null);
  readonly sortField = signal<'question' | 'type'>('question');
  readonly sortDir = signal<'asc' | 'desc'>('asc');

  ngOnInit(): void {
    if (Number.isNaN(this.deckId)) {
      this.router.navigate(['/decks']);
      return;
    }
    this.loadAll();
  }

  openCreateCardForm(): void {
    this.cardToEdit.set(null);
    this.isCardFormOpen.set(true);
  }

  openEditCardForm(card: CardResponse): void {
    this.cardToEdit.set(card);
    this.isCardFormOpen.set(true);
  }

  onCardSaved(card: CardResponse): void {
    const isEdit = this.cardToEdit() !== null;
    if (isEdit) {
      this.isCardFormOpen.set(false);
      this.cardToEdit.set(null);
      this.cards.update((current) => current.map((c) => (c.id === card.id ? card : c)));
      this.toastService.success(this.transloco.translate('deckDetail.toast.cardUpdated'));
    } else {
      this.toastService.success(this.transloco.translate('deckDetail.toast.cardCreated'));
      this.loadCards();
    }
    this.loadStats();
    this.loadTags(); // refresh tag list so newly created tags appear in the dropdown
  }

  requestDeleteCard(card: CardResponse): void {
    this.cardToDelete.set(card);
  }

  confirmDeleteCard(): void {
    const card = this.cardToDelete();
    if (!card) return;
    this.cardToDelete.set(null);
    this.service.deleteCard(this.deckId, card.id).subscribe({
      complete: () => {
        this.isCardFormOpen.set(false);
        this.cardToEdit.set(null);
        this.toastService.success(this.transloco.translate('deckDetail.toast.cardDeleted'));
        this.loadCards();
        this.loadStats();
        this.loadTags(); // orphaned tags were deleted on the backend, keep dropdown in sync
      },
      error: () => this.toastService.error(this.transloco.translate('common.error')),
    });
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadCards();
  }

  toggleSort(field: 'question' | 'type'): void {
    if (this.sortField() === field) {
      this.sortDir.update((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortField.set(field);
      this.sortDir.set('asc');
    }
    this.currentPage.set(0);
    this.loadCards();
  }

  private loadAll(): void {
    this.loadDeck();
    this.loadStats();
    this.loadCards();
    this.loadTags();
  }

  private loadDeck(): void {
    this.service.getDeck(this.deckId).subscribe({
      next: (deck) => this.deck.set(deck),
      error: () => this.router.navigate(['/decks']),
    });
  }

  private loadStats(): void {
    this.service.getDeckStats(this.deckId).subscribe({
      next: (stats) => this.stats.set(stats),
    });
  }

  private loadCards(): void {
    this.isLoading.set(true);
    this.service
      .getCards(this.deckId, this.currentPage(), PAGE_SIZE, this.sortField(), this.sortDir())
      .subscribe({
        next: (page) => {
          this.cards.set(page.content);
          this.totalPages.set(page.totalPages);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.toastService.error(this.transloco.translate('common.error'));
        },
      });
  }

  private loadTags(): void {
    this.service.getTags().subscribe({
      next: (tags) => this.tags.set(tags),
    });
  }
}
