import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LucideBookOpen, LucideChevronRight, LucidePlus } from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { CardsTableComponent } from '../cards-table/cards-table.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { DeckDetailService } from '../../services/deck-detail.service';
import { DeckResponse, DeckStats } from '@core/models/deck.model';
import { NoteEditorComponent } from '../note-editor/note-editor.component';
import { NoteResponse } from '@core/models/note.model';
import { NoteService } from '../../services/note.service';
import { ToastService } from '@core/toast/toast.service';
import AppBadgeComponent from '@shared/components/ui/badge/badge.component';
import AppButtonComponent from '@shared/components/ui/button/button.component';
import PaginationComponent from '@shared/components/pagination/pagination.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-deck-detail',
  imports: [
    AppBadgeComponent,
    AppButtonComponent,
    CardsTableComponent,
    ConfirmDialogComponent,
    DatePipe,
    LucideBookOpen,
    LucideChevronRight,
    LucidePlus,
    NoteEditorComponent,
    PaginationComponent,
    RouterLink,
    TranslocoPipe,
  ],
  templateUrl: './deck-detail.component.html',
})
export default class DeckDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(DeckDetailService);
  private readonly noteService = inject(NoteService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly deckId = Number(this.route.snapshot.paramMap.get('id'));

  readonly deck = signal<DeckResponse | null>(null);
  readonly stats = signal<DeckStats | null>(null);
  readonly notes = signal<NoteResponse[]>([]);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly isLoading = signal(true);
  readonly isNoteEditorOpen = signal(false);
  readonly noteToEdit = signal<NoteResponse | null>(null);
  readonly noteToDelete = signal<NoteResponse | null>(null);
  readonly sortField = signal<'type' | 'createdAt'>('createdAt');
  readonly sortDir = signal<'asc' | 'desc'>('desc');

  ngOnInit(): void {
    if (Number.isNaN(this.deckId)) {
      this.router.navigate(['/decks']);
      return;
    }
    this.loadAll();
  }

  openCreateNoteEditor(): void {
    this.noteToEdit.set(null);
    this.isNoteEditorOpen.set(true);
  }

  openEditNoteEditor(note: NoteResponse): void {
    this.noteToEdit.set(note);
    this.isNoteEditorOpen.set(true);
  }

  closeNoteEditor(): void {
    this.isNoteEditorOpen.set(false);
    this.noteToEdit.set(null);
  }

  onNoteSaved(note: NoteResponse): void {
    const isEdit = this.noteToEdit() !== null;
    if (isEdit) {
      this.isNoteEditorOpen.set(false);
      this.noteToEdit.set(null);
      this.notes.update((current) => current.map((n) => (n.id === note.id ? note : n)));
      this.toastService.success(this.transloco.translate('deckDetail.toast.noteUpdated'));
    } else {
      this.toastService.success(this.transloco.translate('deckDetail.toast.noteCreated'));
      this.loadNotes();
    }
    this.loadStats();
  }

  requestDeleteNote(note: NoteResponse): void {
    this.noteToDelete.set(note);
  }

  confirmDeleteNote(): void {
    const note = this.noteToDelete();
    if (!note) return;
    this.noteToDelete.set(null);
    this.noteService.deleteNote(this.deckId, note.id).subscribe({
      complete: () => {
        this.isNoteEditorOpen.set(false);
        this.noteToEdit.set(null);
        this.toastService.success(this.transloco.translate('deckDetail.toast.noteDeleted'));
        this.loadNotes();
        this.loadStats();
      },
      error: () => this.toastService.error(this.transloco.translate('common.error')),
    });
  }

  goToPage(page: number): void {
    this.currentPage.set(page);
    this.loadNotes();
  }

  toggleSort(field: 'type' | 'createdAt'): void {
    if (this.sortField() === field) {
      this.sortDir.update((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortField.set(field);
      this.sortDir.set('asc');
    }
    this.currentPage.set(0);
    this.loadNotes();
  }

  private loadAll(): void {
    this.loadDeck();
    this.loadStats();
    this.loadNotes();
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

  private loadNotes(): void {
    this.isLoading.set(true);
    this.noteService
      .getNotesByDeck(this.deckId, this.currentPage(), PAGE_SIZE, this.sortField(), this.sortDir())
      .subscribe({
        next: (page) => {
          this.notes.set(page.content);
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
