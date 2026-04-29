import { Component, input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  imports: [],
  templateUrl: './stat-card.component.html',
})
export class StatCardComponent {
  readonly value = input.required<number>();
  readonly label = input.required<string>();
}
