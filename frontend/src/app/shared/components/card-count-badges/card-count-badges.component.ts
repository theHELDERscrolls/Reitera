import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-card-count-badges',
  imports: [],
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

  readonly containerClass = computed(() =>
    this.size() === 'lg' ? 'flex items-center gap-2' : 'flex items-center gap-1.5',
  );

  readonly badgeBase = computed(() =>
    this.size() === 'lg'
      ? 'px-2 py-0.5 text-xs sm:text-base font-semibold rounded'
      : 'min-w-7 px-1 py-0.5 text-xs font-semibold text-center rounded',
  );
}
