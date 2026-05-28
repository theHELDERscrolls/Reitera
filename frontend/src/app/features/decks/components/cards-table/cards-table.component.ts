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

import { NoteResponse } from '@core/models/note.model';
import { CardTypeBadgeComponent } from '@shared/components/card-type-badge/card-type-badge.component';
import { EmptyStateComponent } from '@shared/components/empty-state/empty-state.component';

@Component({
  selector: 'app-cards-table',
  imports: [
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
  readonly notes = input.required<NoteResponse[]>();
  readonly isLoading = input.required<boolean>();
  readonly sortDir = input.required<'asc' | 'desc'>();
  readonly sortField = input.required<'type' | 'createdAt'>();

  readonly emptyTitle = input<string>('deckDetail.cards.empty.title');
  readonly emptySubtitle = input<string>('deckDetail.cards.empty.subtitle');
  readonly showAddAction = input<boolean>(true);

  readonly addNote = output<void>();
  readonly delete = output<NoteResponse>();
  readonly edit = output<NoteResponse>();
  readonly sort = output<'type' | 'createdAt'>();

  firstLine(content: string): string {
    return content.split('\n').find((l) => l.trim()) ?? '';
  }
}
