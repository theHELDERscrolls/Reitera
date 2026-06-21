import { Component, computed, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { CardType } from '@core/models/card.model';
import { NoteType } from '@core/models/note.model';
import AppBadgeComponent, { BadgeVariant } from '../ui/badge/badge.component';

type BadgeType = CardType | NoteType;

const VARIANT_MAP: Record<string, BadgeVariant> = {
  BASIC: 'info',
  BASIC_REVERSE: 'accent-2',
  CLOZE: 'accent-4',
  MULTIPLE_CHOICE: 'success',
};

@Component({
  selector: 'app-card-type-badge',
  imports: [TranslocoPipe, AppBadgeComponent],
  templateUrl: './card-type-badge.component.html',
})
export class CardTypeBadgeComponent {
  readonly type = input.required<BadgeType>();

  readonly badgeVariant = computed<BadgeVariant>(() => VARIANT_MAP[this.type()] ?? 'default');
}
