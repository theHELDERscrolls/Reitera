import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideX } from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import AppButtonComponent from '@shared/components/ui/button/button.component';
import AppInputComponent from '@shared/components/ui/input/input.component';
import { Category } from '@core/models/category.model';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { DeckRequest, DeckResponse } from '@core/models/deck.model';
import { DecksService } from '../../services/decks.service';
import { ToastService } from '@core/toast/toast.service';

@Component({
  selector: 'app-deck-form',
  imports: [AppButtonComponent, AppInputComponent, ConfirmDialogComponent, LucideX, ReactiveFormsModule, TranslocoPipe],
  templateUrl: './deck-form.component.html',
})
export class DeckFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly decksService = inject(DecksService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly deckToEdit = input<DeckResponse | null>(null);
  readonly categories = input.required<Category[]>();

  readonly saved = output<DeckResponse>();
  readonly closed = output<void>();

  readonly isSaving = signal(false);
  readonly isPendingConfirm = signal(false);

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', Validators.maxLength(2000)],
    categoryName: ['', Validators.maxLength(50)],
  });

  constructor() {
    effect(() => {
      const deck = this.deckToEdit();
      if (deck) {
        this.form.patchValue({
          title: deck.title,
          description: deck.description ?? '',
          categoryName: deck.categoryName ?? '',
        });
      } else {
        this.form.reset({ title: '', description: '', categoryName: '' });
      }
    });
  }

  get isEditMode(): boolean {
    return this.deckToEdit() !== null;
  }

  get titleControl() {
    return this.form.controls.title;
  }

  submit(): void {
    if (this.form.invalid || this.isSaving()) return;
    if (this.isEditMode) {
      this.isPendingConfirm.set(true);
      return;
    }
    this.doSave();
  }

  confirmSave(): void {
    this.isPendingConfirm.set(false);
    this.doSave();
  }

  private doSave(): void {
    const { title, description, categoryName } = this.form.getRawValue();
    const trimmedName = categoryName?.trim() ?? '';
    const matchedCat = this.categories().find(
      (c) => c.name.toLowerCase() === trimmedName.toLowerCase(),
    );

    const request: DeckRequest = {
      title: title!,
      description: description ?? '',
      categoryId: matchedCat?.id ?? null,
      categoryName: !matchedCat && trimmedName ? trimmedName : null,
    };

    const deck = this.deckToEdit();
    const operation = deck
      ? this.decksService.updateDeck(deck.id, request)
      : this.decksService.createDeck(request);

    this.isSaving.set(true);
    operation.subscribe({
      next: (result) => {
        this.isSaving.set(false);
        this.saved.emit(result);
      },
      error: () => {
        this.isSaving.set(false);
        this.toastService.error(this.transloco.translate('common.error'));
      },
    });
  }
}
