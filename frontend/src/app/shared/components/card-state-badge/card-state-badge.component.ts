import { Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-card-state-badge',
  imports: [TranslocoPipe],
  templateUrl: './card-state-badge.component.html',
})
export class CardStateBadgeComponent {
  readonly state = input.required<number | null>();
}
