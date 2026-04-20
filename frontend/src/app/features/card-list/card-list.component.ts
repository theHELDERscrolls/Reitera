import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideListFilter, LucideX } from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { CardFilters, CardsService } from './services/cards.service';
import { CardFormComponent } from '@features/decks/components/card-form/card-form.component';
import { CardResponse } from '@core/models/card.model';
import { CardsTableComponent } from '@features/decks/components/cards-table/cards-table.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import {
  FilterDropdownComponent,
  FilterOption,
} from '@shared/components/filter-dropdown/filter-dropdown.component';
import { Tag } from '@core/models/tag.model';
import { ToastService } from '@core/toast/toast.service';
import PaginationComponent from '@shared/components/pagination/pagination.component';

const PAGE_SIZE = 20;

type FilterSelectValue = string | number | null;

@Component({
  selector: 'app-card-list',
  imports: [
    CardFormComponent,
    CardsTableComponent,
    ConfirmDialogComponent,
    FilterDropdownComponent,
    FormsModule,
    LucideListFilter,
    LucideX,
    PaginationComponent,
    TranslocoPipe,
  ],
  templateUrl: './card-list.component.html',
})
export default class CardListComponent implements OnInit {
  private readonly service = inject(CardsService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly cards = signal<CardResponse[]>([]);
  readonly cardToDelete = signal<CardResponse | null>(null);
  readonly cardToEdit = signal<CardResponse | null>(null);
  readonly currentPage = signal(0);
  readonly isCardFormOpen = signal(false);
  readonly isLoading = signal(true);
  readonly sortDir = signal<'asc' | 'desc'>('asc');
  readonly sortField = signal<'question' | 'type'>('question');
  readonly tags = signal<Tag[]>([]);
  readonly totalPages = signal(0);

  readonly filterQuestion = signal('');
  readonly filterType = signal<string | null>(null);
  readonly filterState = signal<number | null>(null);
  readonly filterTagId = signal<number | null>(null);

  readonly typeOptions: FilterOption[] = [
    { value: null, labelKey: 'cardList.allTypes' },
    { value: 'BASIC', labelKey: 'study.session.type.BASIC' },
    { value: 'MULTIPLE_CHOICE', labelKey: 'study.session.type.MULTIPLE_CHOICE' },
    { value: 'TRUE_FALSE', labelKey: 'study.session.type.TRUE_FALSE' },
  ];

  readonly stateOptions: FilterOption[] = [
    { value: null, labelKey: 'cardList.allStates' },
    { value: -1, labelKey: 'cardList.stateNotStudied' },
    { value: 0, labelKey: 'cardList.stateNew' },
    { value: 1, labelKey: 'cardList.stateLearning' },
    { value: 2, labelKey: 'cardList.stateReview' },
    { value: 3, labelKey: 'cardList.stateRelearning' },
  ];

  readonly tagOptions = computed<FilterOption[]>(() => [
    { value: null, labelKey: 'cardList.allTags' },
    ...this.tags().map((t) => ({ value: t.id, label: t.name, colorHex: t.hexColor })),
  ]);

  readonly hasActiveFilters = computed(
    () =>
      !!(
        this.filterQuestion() ||
        this.filterType() ||
        this.filterState() !== null ||
        this.filterTagId() !== null
      ),
  );

  ngOnInit(): void {
    this.loadTags();
    this.loadCards();
  }

  // Card form actions

  openEditCardForm(card: CardResponse): void {
    this.cardToEdit.set(card);
    this.isCardFormOpen.set(true);
  }

  closeCardForm(): void {
    this.isCardFormOpen.set(false);
    this.cardToEdit.set(null);
  }

  onCardSaved(card: CardResponse): void {
    this.isCardFormOpen.set(false);
    this.cardToEdit.set(null);
    this.cards.update((current) => current.map((c) => (c.id === card.id ? card : c)));
    this.toastService.success(this.transloco.translate('cardList.toast.cardUpdated'));
  }

  // Delete actions

  requestDeleteCard(card: CardResponse): void {
    this.cardToDelete.set(card);
  }

  confirmDeleteCard(): void {
    const card = this.cardToDelete();

    if (!card) return;

    this.cardToDelete.set(null);
    this.service.deleteCard(card.deckId, card.id).subscribe({
      complete: () => {
        this.toastService.success(this.transloco.translate('cardList.toast.cardDeleted'));
        this.loadCards();
      },
      error: () => this.toastService.error(this.transloco.translate('common.error')),
    });
  }

  // Sorting & pagination

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

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadCards();
  }

  // Filter actions

  selectType(value: FilterSelectValue): void {
    this.filterType.set(value as string | null);
    this.applyFilters();
  }

  selectState(value: FilterSelectValue): void {
    this.filterState.set(value as number | null);
    this.applyFilters();
  }

  selectTag(value: FilterSelectValue): void {
    this.filterTagId.set(value as number | null);
    this.applyFilters();
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.loadCards();
  }

  clearFilters(): void {
    this.filterQuestion.set('');
    this.filterType.set(null);
    this.filterState.set(null);
    this.filterTagId.set(null);
    this.currentPage.set(0);
    this.loadCards();
  }

  // Private helpers

  private loadCards(): void {
    this.isLoading.set(true);

    const filters: CardFilters = {
      question: this.filterQuestion() || undefined,
      type: this.filterType(),
      state: this.filterState(),
      tagId: this.filterTagId(),
    };

    this.service
      .getCards(filters, this.currentPage(), PAGE_SIZE, this.sortField(), this.sortDir())
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
