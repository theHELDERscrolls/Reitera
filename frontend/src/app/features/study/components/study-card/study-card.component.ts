import { Component, computed, effect, input, output, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { DueCard } from '@core/models/study.model';
import { CardExplanationComponent } from '../card-explanation/card-explanation.component';
import { RatingButtonsComponent } from '../rating-buttons/rating-buttons.component';

@Component({
  selector: 'app-study-card',
  imports: [TranslocoPipe, CardExplanationComponent, RatingButtonsComponent],
  templateUrl: './study-card.component.html',
})
export class StudyCardComponent {
  readonly card = input.required<DueCard>();
  readonly revealed = input.required<boolean>();

  readonly reveal = output<void>();
  readonly rate = output<1 | 2 | 3 | 4>();

  readonly selectedMcIndex = signal<number | null>(null);
  readonly selectedTf = signal<boolean | null>(null);

  constructor() {
    effect(() => {
      this.card();
      this.selectedMcIndex.set(null);
      this.selectedTf.set(null);
    });
  }

  readonly mcOptions = computed(() => {
    const json = this.card().answerJson as { options: string[]; correctIndex: number };

    const mapped = (json.options ?? []).map((text, i) => ({
      text,
      correct: i === json.correctIndex,
      index: i,
    }));

    for (let i = mapped.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [mapped[i], mapped[j]] = [mapped[j], mapped[i]];
    }

    return mapped;
  });

  readonly tfCorrect = computed(() => {
    const json = this.card().answerJson as { correct: boolean };

    return json.correct;
  });

  readonly basicAnswer = computed(() => {
    const json = this.card().answerJson as { answer: string };

    return json.answer ?? '';
  });

  selectMcOption(index: number): void {
    if (this.revealed()) return;

    this.selectedMcIndex.set(index);
    this.reveal.emit();
  }

  selectTf(value: boolean): void {
    if (this.revealed()) return;

    this.selectedTf.set(value);
    this.reveal.emit();
  }
}
