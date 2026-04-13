import { Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { CardType } from '@core/models/card.model';

@Component({
  selector: 'app-card-type-badge',
  imports: [TranslocoPipe],
  templateUrl: './card-type-badge.component.html',
})
export class CardTypeBadgeComponent {
  readonly type = input.required<CardType>();
}
