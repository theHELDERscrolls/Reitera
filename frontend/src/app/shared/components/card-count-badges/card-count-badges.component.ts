import { Component, computed, input } from '@angular/core';

import { BadgeSize } from '@shared/components/ui/badge/badge.component';
import AppBadgeComponent from '@shared/components/ui/badge/badge.component';

@Component({
  selector: 'app-card-count-badges',
  imports: [AppBadgeComponent],
  templateUrl: './card-count-badges.component.html',
})
export class CardCountBadgesComponent {
  readonly newCount = input.required<number>();
  readonly dueCount = input.required<number>();
  readonly relearningCount = input.required<number>();
  readonly size = input<'sm' | 'lg'>('sm');

  readonly newLabel = input<string>();
  readonly dueLabel = input<string>();
  readonly relearningLabel = input<string>();

  readonly badgeSize = computed<BadgeSize>(() => this.size() === 'lg' ? 'md' : 'sm');
  readonly containerClass = computed(() =>
    this.size() === 'lg' ? 'flex items-center gap-2' : 'flex items-center gap-1.5',
  );
}
