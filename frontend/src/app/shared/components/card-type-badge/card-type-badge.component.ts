import { Component, computed, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { CardType } from '@core/models/card.model';

@Component({
  selector: 'app-card-type-badge',
  imports: [TranslocoPipe],
  templateUrl: './card-type-badge.component.html',
})
export class CardTypeBadgeComponent {
  readonly type = input.required<CardType>();

  readonly badgeClass = computed(() => {
    const colorMap: Record<CardType, string> = {
      BASIC: 'bg-info/10 text-info',
      BASIC_REVERSE: 'bg-accent-2/10 text-accent-2',
      CLOZE: 'bg-accent-4/10 text-accent-4',
      MULTIPLE_CHOICE: 'bg-success/10 text-success',
    };
    return colorMap[this.type()] ?? 'bg-overlay text-muted';
  });
}
