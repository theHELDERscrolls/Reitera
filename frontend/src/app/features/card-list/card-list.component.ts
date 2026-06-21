import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  LucideArrowDown,
  LucideArrowUp,
  LucideArrowUpDown,
  LucideBookOpen,
  LucideListFilter,
  LucideSquarePen,
  LucideTrash2,
  LucideX,
} from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import AppButtonComponent from '@shared/components/ui/button/button.component';
import { CardFilters, CardsService } from './services/cards.service';
import { CardResponse } from '@core/models/card.model';
import { CardStateBadgeComponent } from '@shared/components/card-state-badge/card-state-badge.component';
import { CardTypeBadgeComponent } from '@shared/components/card-type-badge/card-type-badge.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';
import {
  FilterDropdownComponent,
  FilterOption,
} from '@shared/components/filter-dropdown/filter-dropdown.component';
import { NoteEditorComponent } from '@features/decks/components/note-editor/note-editor.component';
import { NoteResponse } from '@core/models/note.model';
import { NoteService } from '@features/decks/services/note.service';
import { ToastService } from '@core/toast/toast.service';
import PaginationComponent from '@shared/components/pagination/pagination.component';

const PAGE_SIZE = 20;

type FilterSelectValue = string | number | null;

@Component({
  selector: 'app-card-list',
  imports: [
    AppButtonComponent,
    CardStateBadgeComponent,
    CardTypeBadgeComponent,
    ConfirmDialogComponent,
    EmptyStateComponent,
    FilterDropdownComponent,
    FormsModule,
    LucideArrowDown,
    LucideArrowUp,
    LucideArrowUpDown,
    LucideBookOpen,
    LucideListFilter,
    LucideSquarePen,
    LucideTrash2,
    LucideX,
    NoteEditorComponent,
    PaginationComponent,
    TranslocoPipe,
  ],
  templateUrl: './card-list.component.html',
})
export default class CardListComponent implements OnInit {
  private readonly service = inject(CardsService);
  private readonly noteService = inject(NoteService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly cards = signal<CardResponse[]>([]);
  readonly cardToDelete = signal<CardResponse | null>(null);
  readonly noteToEdit = signal<NoteResponse | null>(null);
  readonly currentPage = signal(0);
  readonly isNoteEditorOpen = signal(false);
  readonly isLoading = signal(true);
  readonly sortDir = signal<'asc' | 'desc'>('asc');
  readonly sortField = signal<'question' | 'type'>('question');
  readonly totalPages = signal(0);

  readonly filterQuestion = signal('');
  readonly filterType = signal<string | null>(null);
  readonly filterState = signal<number | null>(null);

  readonly typeOptions: FilterOption[] = [
    { value: null, labelKey: 'cardList.allTypes' },
    { value: 'BASIC', labelKey: 'study.session.type.BASIC' },
    { value: 'BASIC_REVERSE', labelKey: 'study.session.type.BASIC_REVERSE' },
    { value: 'CLOZE', labelKey: 'study.session.type.CLOZE' },
    { value: 'MULTIPLE_CHOICE', labelKey: 'study.session.type.MULTIPLE_CHOICE' },
  ];

  readonly stateOptions: FilterOption[] = [
    { value: null, labelKey: 'cardList.allStates' },
    { value: -1, labelKey: 'cardList.stateNew' },
    { value: 1, labelKey: 'cardList.stateLearning' },
    { value: 2, labelKey: 'cardList.stateReview' },
    { value: 3, labelKey: 'cardList.stateRelearning' },
  ];

  readonly hasActiveFilters = computed(
    () => !!(this.filterQuestion() || this.filterType() || this.filterState() !== null),
  );

  ngOnInit(): void {
    this.loadCards();
  }

  openEditNoteEditor(card: CardResponse): void {
    this.noteService.getNoteById(card.deckId, card.noteId).subscribe({
      next: (note) => {
        this.noteToEdit.set(note);
        this.isNoteEditorOpen.set(true);
      },
      error: () => this.toastService.error(this.transloco.translate('common.error')),
    });
  }

  closeNoteEditor(): void {
    this.isNoteEditorOpen.set(false);
    this.noteToEdit.set(null);
  }

  onNoteSaved(): void {
    this.isNoteEditorOpen.set(false);
    this.noteToEdit.set(null);
    this.toastService.success(this.transloco.translate('cardList.toast.noteUpdated'));
    this.loadCards();
  }

  requestDeleteCard(card: CardResponse): void {
    this.cardToDelete.set(card);
  }

  confirmDeleteCard(): void {
    const card = this.cardToDelete();
    if (!card) return;
    this.cardToDelete.set(null);
    this.noteService.deleteNote(card.deckId, card.noteId).subscribe({
      complete: () => {
        this.toastService.success(this.transloco.translate('cardList.toast.noteDeleted'));
        this.loadCards();
      },
      error: () => this.toastService.error(this.transloco.translate('common.error')),
    });
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

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadCards();
  }

  selectType(value: FilterSelectValue): void {
    this.filterType.set(value as string | null);
    this.applyFilters();
  }

  selectState(value: FilterSelectValue): void {
    this.filterState.set(value as number | null);
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
    this.currentPage.set(0);
    this.loadCards();
  }

  private loadCards(): void {
    this.isLoading.set(true);

    const filters: CardFilters = {
      question: this.filterQuestion() || undefined,
      type: this.filterType(),
      state: this.filterState(),
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
}
