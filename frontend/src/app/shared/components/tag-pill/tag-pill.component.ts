import { Component, input, output } from '@angular/core';
import { LucideX } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-tag-pill',
  imports: [LucideX, TranslocoPipe],
  templateUrl: './tag-pill.component.html',
})
export class TagPillComponent {
  readonly name = input.required<string>();
  readonly hexColor = input.required<string>();
  readonly removable = input<boolean>(false);

  readonly remove = output<void>();
}
