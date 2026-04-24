import { Component, input } from '@angular/core';
import { LucidePlay } from '@lucide/angular';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { CardCountBadgesComponent } from '@shared/components/card-count-badges/card-count-badges.component';
import { LastStudiedDeck } from '@core/models/dashboard.model';

@Component({
  selector: 'app-last-studied-decks',
  imports: [RouterLink, TranslocoPipe, LucidePlay, CardCountBadgesComponent],
  templateUrl: './last-studied-decks.component.html',
})
export class LastStudiedDecksComponent {
  readonly decks = input.required<LastStudiedDeck[]>();
  readonly loading = input.required<boolean>();

  readonly skeletons = [0, 1, 2];

  hasCards(deck: LastStudiedDeck): boolean {
    return deck.dueCount > 0 || deck.newCount > 0 || deck.relearningCount > 0;
  }
}
