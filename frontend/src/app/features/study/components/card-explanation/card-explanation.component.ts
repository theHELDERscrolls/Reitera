import { Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { MarkdownComponent } from 'ngx-markdown';

@Component({
  selector: 'app-card-explanation',
  imports: [TranslocoPipe, MarkdownComponent],
  templateUrl: './card-explanation.component.html',
})
export class CardExplanationComponent {
  readonly text = input.required<string>();
}
