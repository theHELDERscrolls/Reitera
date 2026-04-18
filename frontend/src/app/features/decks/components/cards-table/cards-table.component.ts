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
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-cards-table',
  imports: [
    CardStateBadgeComponent,
    CardTypeBadgeComponent,
    EmptyStateComponent,
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
  readonly isLoading = input.required<boolean>();
  readonly sortDir = input.required<'asc' | 'desc'>();
  readonly sortField = input.required<'question' | 'type'>();

  readonly addCard = output<void>();
  readonly delete = output<CardResponse>();
  readonly edit = output<CardResponse>();
  readonly sort = output<'question' | 'type'>();
}
