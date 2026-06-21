import { Component, computed, effect, input, output, signal } from '@angular/core';
import { MarkdownComponent } from 'ngx-markdown';
import { TranslocoPipe } from '@jsverse/transloco';

import { CardExplanationComponent } from '../card-explanation/card-explanation.component';
import { DueCard } from '@core/models/study.model';
import { RatingButtonsComponent } from '../rating-buttons/rating-buttons.component';
import AppButtonComponent from '@shared/components/ui/button/button.component';

@Component({
  selector: 'app-study-card',
  imports: [
    AppButtonComponent,
    TranslocoPipe,
    CardExplanationComponent,
    RatingButtonsComponent,
    MarkdownComponent,
  ],
  templateUrl: './study-card.component.html',
})
export class StudyCardComponent {
  readonly card = input.required<DueCard>();
  readonly revealed = input.required<boolean>();

  readonly reveal = output<void>();
  readonly rate = output<1 | 3>();

  readonly selectedMcIndex = signal<number | null>(null);

  constructor() {
    effect(() => {
      this.card();
      this.selectedMcIndex.set(null);
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

  readonly basicAnswer = computed(() => {
    const json = this.card().answerJson as { answer: string };

    return json.answer ?? '';
  });

  selectMcOption(index: number): void {
    if (this.revealed()) return;

    this.selectedMcIndex.set(index);
    this.reveal.emit();
  }
}
