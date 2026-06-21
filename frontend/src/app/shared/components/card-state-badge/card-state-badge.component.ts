import { Component, computed, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import AppBadgeComponent, { BadgeVariant } from '@shared/components/ui/badge/badge.component';

@Component({
  selector: 'app-card-state-badge',
  imports: [TranslocoPipe, AppBadgeComponent],
  templateUrl: './card-state-badge.component.html',
})
export class CardStateBadgeComponent {
  readonly state = input.required<number | null>();

  readonly badgeVariant = computed<BadgeVariant>(() => {
    const s = this.state();
    if (s === null || s === 0) return 'default';
    if (s === 1) return 'info';
    if (s === 2) return 'success';
    return 'warning';
  });

  readonly stateKey = computed(() => {
    const s = this.state();
    if (s === null || s === 0) return 'deckDetail.cards.state.new';
    if (s === 1) return 'deckDetail.cards.state.learning';
    if (s === 2) return 'deckDetail.cards.state.review';
    
    return 'deckDetail.cards.state.relearning';
  });
}
