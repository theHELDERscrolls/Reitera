import { Component, computed, input } from '@angular/core';

export type StatCardColor = 'accent-1' | 'accent-2' | 'accent-3' | 'accent-4' | 'accent-5';

const BG_MAP: Record<StatCardColor, string> = {
  'accent-1': 'bg-accent-1',
  'accent-2': 'bg-accent-2',
  'accent-3': 'bg-accent-3',
  'accent-4': 'bg-accent-4',
  'accent-5': 'bg-accent-5',
};

@Component({
  selector: 'app-stat-card',
  imports: [],
  templateUrl: './stat-card.component.html',
})
export class StatCardComponent {
  readonly value = input.required<number>();
  readonly label = input.required<string>();
  readonly color = input<StatCardColor>('accent-1');

  readonly bgClass = computed(() => BG_MAP[this.color()]);
}
