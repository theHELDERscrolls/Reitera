import { Component, inject, OnInit, signal } from '@angular/core';
import { LucidePlus } from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import PaginationComponent from '@shared/components/pagination/pagination.component';
import { Category } from '@core/models/category.model';
import { CategoryFilterComponent } from './components/category-filter/category-filter.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { DeckCardComponent } from './components/deck-card/deck-card.component';
import { DeckFormComponent } from './components/deck-form/deck-form.component';
import { DeckResponse } from '@core/models/deck.model';
import { DecksService } from './services/decks.service';
import { ToastService } from '@core/toast/toast.service';

const PAGE_SIZE = 12;

@Component({
  selector: 'app-decks',
  imports: [
    CategoryFilterComponent,
    ConfirmDialogComponent,
    DeckCardComponent,
    DeckFormComponent,
    LucidePlus,
    PaginationComponent,
    TranslocoPipe,
  ],
  templateUrl: './decks.component.html',
})
export default class DecksComponent implements OnInit {
  private readonly decksService = inject(DecksService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly activeCategory = signal<string | null>(null);
  readonly categories = signal<Category[]>([]);
  readonly currentPage = signal(0);
  readonly decks = signal<DeckResponse[]>([]);
  readonly deckToDelete = signal<DeckResponse | null>(null);
  readonly deckToEdit = signal<DeckResponse | null>(null);
  readonly isDialogOpen = signal(false);
  readonly isLoading = signal(true);
  readonly totalPages = signal(0);

  ngOnInit(): void {
    this.loadDecks();
  }

  setActiveCategory(value: string | null): void {
    this.activeCategory.set(value);
    this.currentPage.set(0);
    this.loadDecks();
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadDecks();
  }

  openCreateDialog(): void {
    this.deckToEdit.set(null);
    this.isDialogOpen.set(true);
  }

  openEditDialog(deck: DeckResponse): void {
    this.deckToEdit.set(deck);
    this.isDialogOpen.set(true);
  }

  onDeckSaved(deck: DeckResponse): void {
    const isEdit = this.deckToEdit() !== null;
    this.isDialogOpen.set(false);
    this.deckToEdit.set(null);

    if (isEdit) {
      this.decks.update((current) => current.map((d) => (d.id === deck.id ? deck : d)));
      this.toastService.success(this.transloco.translate('decks.toast.updated'));
      this.refreshCategories();
    } else {
      this.toastService.success(this.transloco.translate('decks.toast.created'));
      this.loadDecks();
    }
  }

  requestDeleteDeck(deck: DeckResponse): void {
    this.deckToDelete.set(deck);
  }

  confirmDeleteDeck(): void {
    const deck = this.deckToDelete();
    if (!deck) return;
    this.deckToDelete.set(null);
    this.decksService.deleteDeck(deck.id).subscribe({
      complete: () => {
        this.toastService.success(this.transloco.translate('decks.toast.deleted'));
        this.loadDecks();
      },
      error: () => this.toastService.error(this.transloco.translate('common.error')),
    });
  }

  private loadDecks(): void {
    this.isLoading.set(true);

    const activeCategoryName = this.activeCategory();
    const categoryId = activeCategoryName
      ? this.categories().find((c) => c.name === activeCategoryName)?.id
      : undefined;

    this.decksService.getDecks(this.currentPage(), PAGE_SIZE, categoryId).subscribe({
      next: (page) => {
        this.decks.set(page.content);
        this.totalPages.set(page.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.toastService.error(this.transloco.translate('common.error'));
      },
    });

    this.refreshCategories();
  }

  private refreshCategories(): void {
    this.decksService.getCategories().subscribe({
      next: (cats) => this.categories.set(cats),
    });
  }
}
