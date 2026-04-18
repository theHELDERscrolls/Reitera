import { Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  imports: [],
  templateUrl: './empty-state.component.html',
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly subtitle = input.required<string>();
}
