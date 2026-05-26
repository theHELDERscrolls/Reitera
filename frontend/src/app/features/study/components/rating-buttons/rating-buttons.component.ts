import { Component, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-rating-buttons',
  imports: [TranslocoPipe],
  templateUrl: './rating-buttons.component.html',
})
export class RatingButtonsComponent {
  readonly rate = output<1 | 3>();
}
