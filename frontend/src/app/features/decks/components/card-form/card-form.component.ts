import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { LucideMinus, LucidePlus, LucideTrash2 } from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { CardRequest, CardResponse, CardType } from '@core/models/card.model';
import { DeckDetailService } from '../../services/deck-detail.service';
import { ToastService } from '@core/toast/toast.service';

@Component({
  selector: 'app-card-form',
  imports: [
    LucideMinus,
    LucidePlus,
    LucideTrash2,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './card-form.component.html',
})
export class CardFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(DeckDetailService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly deckId = input.required<number>();
  readonly cardToEdit = input<CardResponse | null>(null);

  readonly saved = output<CardResponse>();
  readonly closed = output<void>();
  readonly deleted = output<void>();

  readonly isSaving = signal(false);

  readonly form = this.fb.group({
    type: ['BASIC' as CardType, Validators.required],
    question: ['', [Validators.required, Validators.maxLength(5000)]],
    explanation: ['', Validators.maxLength(2000)],
    basicAnswer: [''],
    options: this.fb.array([this.fb.control(''), this.fb.control('')]),
    correctIndex: [0],
    trueFalseCorrect: [true],
  });

  readonly maxOptions = 8;

  readonly isEditMode = computed(() => this.cardToEdit() !== null);
  readonly cardTypes: CardType[] = ['BASIC', 'MULTIPLE_CHOICE', 'TRUE_FALSE'];

  get optionsArray(): FormArray<FormControl<string | null>> {
    return this.form.controls.options;
  }

  get typeControl() {
    return this.form.controls.type;
  }

  get questionControl() {
    return this.form.controls.question;
  }

  get explanationControl() {
    return this.form.controls.explanation;
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

  submit(): void {
    if (this.form.invalid || this.isSaving()) return;

    const answerJson = this.buildAnswerJson();
    const { type, question, explanation } = this.form.getRawValue();

    const dto: CardRequest = {
      type: type as CardType,
      question: question ?? '',
      answerJson,
      explanation: explanation?.trim() || null,
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
      const options =
        saved.length >= 2 ? saved : [...saved, ...new Array(2 - saved.length).fill('')];

      while (this.optionsArray.length > 0) this.optionsArray.removeAt(0);

      options.forEach((opt) => this.optionsArray.push(this.fb.control(opt)));

      this.form.controls.correctIndex.setValue((card.answerJson['correctIndex'] as number) ?? 0);
    } else if (card.type === 'TRUE_FALSE') {
      this.form.patchValue({ trueFalseCorrect: (card.answerJson['correct'] as boolean) ?? true });
    }
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
  }
}
