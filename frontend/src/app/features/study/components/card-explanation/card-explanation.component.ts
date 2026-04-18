import { Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-card-explanation',
  imports: [TranslocoPipe],
  templateUrl: './card-explanation.component.html',
})
export class CardExplanationComponent {
  readonly text = input.required<string>();
}
