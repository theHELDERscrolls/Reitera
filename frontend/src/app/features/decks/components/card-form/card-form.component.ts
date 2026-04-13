import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideMinus, LucidePlus, LucideTrash2 } from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { CardRequest, CardResponse, CardType } from '@core/models/card.model';
import { DeckDetailService } from '../../services/deck-detail.service';
import { Tag } from '@core/models/tag.model';
import { TagPillComponent } from '@shared/components/tag-pill/tag-pill.component';
import { ToastService } from '@core/toast/toast.service';

@Component({
  selector: 'app-card-form',
  imports: [LucideMinus, LucidePlus, LucideTrash2, ReactiveFormsModule, TagPillComponent, TranslocoPipe],
  templateUrl: './card-form.component.html',
})
export class CardFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DeckDetailService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly deckId = input.required<number>();
  readonly cardToEdit = input<CardResponse | null>(null);
  readonly tags = input.required<Tag[]>();

  readonly saved = output<CardResponse>();
  readonly closed = output<void>();
  readonly deleted = output<void>();

  readonly TAG_PALETTE = [
    '#3b82f6', '#22c55e', '#ef4444', '#a855f7',
    '#ec4899', '#f97316', '#14b8a6', '#06b6d4',
    '#6366f1', '#f59e0b',
  ];

  readonly isSaving = signal(false);

  readonly selectedTagIds = signal<number[]>([]);
  readonly pendingNewTags = signal<{ name: string; hexColor: string }[]>([]);
  readonly tagInput = signal('');
  readonly isTagDropdownOpen = signal(false);
  readonly pendingColor = signal('#3b82f6');

  readonly selectedTags = computed(() =>
    this.selectedTagIds()
      .map((id) => this.tags().find((t) => t.id === id))
      .filter((t): t is Tag => t !== undefined),
  );

  readonly filteredTags = computed(() => {
    const q = this.tagInput().toLowerCase().trim();
    const selectedIds = this.selectedTagIds();
    return this.tags().filter(
      (t) => !selectedIds.includes(t.id) && (q === '' || t.name.toLowerCase().includes(q)),
    );
  });

  readonly canCreateNew = computed(() => {
    const q = this.tagInput().trim();
    if (!q) return false;
    const alreadyExists = this.tags().some((t) => t.name.toLowerCase() === q.toLowerCase());
    const alreadyPending = this.pendingNewTags().some(
      (t) => t.name.toLowerCase() === q.toLowerCase(),
    );
    return !alreadyExists && !alreadyPending;
  });

  readonly form = this.fb.group({
    type: ['BASIC' as CardType, Validators.required],
    question: ['', [Validators.required, Validators.maxLength(500)]],
    explanation: [''],
    basicAnswer: [''],
    options: this.fb.array([this.fb.control(''), this.fb.control('')]),
    correctIndex: [0],
    trueFalseCorrect: [true],
  });

  readonly maxOptions = 8;

  readonly isEditMode = computed(() => this.cardToEdit() !== null);
  readonly cardTypes: CardType[] = ['BASIC', 'CLOZE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'];

  get optionsArray(): FormArray<FormControl<string | null>> {
    return this.form.controls.options;
  }

  get typeControl() {
    return this.form.controls.type;
  }

  get questionControl() {
    return this.form.controls.question;
  }

  constructor() {
    effect(() => {
      const card = this.cardToEdit();
      if (card) {
        this.populateForm(card);
      } else {
        this.resetForm();
      }
    });
  }

  addOption(): void {
    if (this.optionsArray.length < this.maxOptions) {
      this.optionsArray.push(this.fb.control(''));
    }
  }

  removeOption(index: number): void {
    if (this.optionsArray.length <= 2) return;
    const current = this.form.controls.correctIndex.value ?? 0;
    if (current === index) {
      this.form.controls.correctIndex.setValue(0);
    } else if (current > index) {
      this.form.controls.correctIndex.setValue(current - 1);
    }
    this.optionsArray.removeAt(index);
  }

  openTagDropdown(): void {
    this.isTagDropdownOpen.set(true);
  }

  closeTagDropdown(): void {
    setTimeout(() => this.isTagDropdownOpen.set(false), 150);
  }

  selectExistingTag(tag: Tag): void {
    this.selectedTagIds.update((ids) => [...ids, tag.id]);
    this.tagInput.set('');
    this.isTagDropdownOpen.set(false);
  }

  removeSelectedTag(tagId: number): void {
    this.selectedTagIds.update((ids) => ids.filter((id) => id !== tagId));
  }

  addNewTag(): void {
    const name = this.tagInput().trim();
    if (!name || !this.canCreateNew()) return;
    this.pendingNewTags.update((tags) => [...tags, { name, hexColor: this.pendingColor() }]);
    this.tagInput.set('');
    this.isTagDropdownOpen.set(false);
  }

  removeNewTag(index: number): void {
    this.pendingNewTags.update((tags) => tags.filter((_, i) => i !== index));
  }

  submit(): void {
    if (this.form.invalid || this.isSaving()) return;

    const answerJson = this.buildAnswerJson();
    const { type, question, explanation } = this.form.getRawValue();

    const pending = this.pendingNewTags();
    const dto: CardRequest = {
      type: type as CardType,
      question: question ?? '',
      answerJson,
      explanation: explanation?.trim() || null,
      tagIds: this.selectedTagIds(),
      newTags: pending.length > 0 ? pending : undefined,
    };

    this.isSaving.set(true);
    const card = this.cardToEdit();
    const operation = card
      ? this.service.updateCard(this.deckId(), card.id, dto)
      : this.service.createCard(this.deckId(), dto);

    operation.subscribe({
      next: (result) => {
        this.isSaving.set(false);
        if (!this.isEditMode()) {
          this.resetForm();
        }
        this.saved.emit(result);
      },
      error: () => {
        this.isSaving.set(false);
        this.toastService.error(this.transloco.translate('common.error'));
      },
    });
  }

  private buildAnswerJson(): Record<string, unknown> {
    const v = this.form.getRawValue();
    if (v.type === 'BASIC') {
      return { answer: v.basicAnswer ?? '' };
    }
    if (v.type === 'CLOZE') {
      return {};
    }
    if (v.type === 'MULTIPLE_CHOICE') {
      return {
        options: this.optionsArray.controls.map((c) => c.value ?? ''),
        correctIndex: v.correctIndex ?? 0,
      };
    }
    return { correct: v.trueFalseCorrect ?? true };
  }

  private populateForm(card: CardResponse): void {
    this.form.patchValue({
      type: card.type,
      question: card.question,
      explanation: card.explanation ?? '',
    });

    if (card.type === 'BASIC') {
      this.form.patchValue({ basicAnswer: (card.answerJson['answer'] as string) ?? '' });
    } else if (card.type === 'MULTIPLE_CHOICE') {
      const saved = (card.answerJson['options'] as string[]) ?? [];
      const options = saved.length >= 2 ? saved : [...saved, ...new Array(2 - saved.length).fill('')];
      while (this.optionsArray.length > 0) this.optionsArray.removeAt(0);
      options.forEach((opt) => this.optionsArray.push(this.fb.control(opt)));
      this.form.controls.correctIndex.setValue((card.answerJson['correctIndex'] as number) ?? 0);
    } else if (card.type === 'TRUE_FALSE') {
      this.form.patchValue({ trueFalseCorrect: (card.answerJson['correct'] as boolean) ?? true });
    }

    this.selectedTagIds.set(card.tags.map((t) => t.id));
    this.pendingNewTags.set([]);
    this.tagInput.set('');
  }

  private resetForm(): void {
    this.form.setControl('options', this.fb.array([this.fb.control(''), this.fb.control('')]));
    this.form.patchValue({
      type: 'BASIC',
      question: '',
      explanation: '',
      basicAnswer: '',
      correctIndex: 0,
      trueFalseCorrect: true,
    });
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.selectedTagIds.set([]);
    this.pendingNewTags.set([]);
    this.tagInput.set('');
  }
}
