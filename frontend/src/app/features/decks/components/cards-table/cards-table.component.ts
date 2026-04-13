import {
  LucideArrowDown,
  LucideArrowUp,
  LucideArrowUpDown,
  LucideBookOpen,
  LucidePlus,
  LucideSquarePen,
  LucideTrash2,
} from '@lucide/angular';
import { Component, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { CardResponse } from '@core/models/card.model';
import { CardStateBadgeComponent } from '@shared/components/card-state-badge/card-state-badge.component';
import { CardTypeBadgeComponent } from '@shared/components/card-type-badge/card-type-badge.component';

@Component({
  selector: 'app-cards-table',
  imports: [
    CardStateBadgeComponent,
    CardTypeBadgeComponent,
    LucideArrowDown,
    LucideArrowUp,
    LucideArrowUpDown,
    LucideBookOpen,
    LucidePlus,
    LucideSquarePen,
    LucideTrash2,
    TranslocoPipe,
  ],
  templateUrl: './cards-table.component.html',
})
export class CardsTableComponent {
  readonly cards = input.required<CardResponse[]>();
  readonly sortField = input.required<'question' | 'type'>();
  readonly sortDir = input.required<'asc' | 'desc'>();
  readonly isLoading = input.required<boolean>();

  readonly sort = output<'question' | 'type'>();
  readonly edit = output<CardResponse>();
  readonly delete = output<CardResponse>();
  readonly addCard = output<void>();
}
