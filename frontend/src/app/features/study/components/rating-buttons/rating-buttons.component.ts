import { Component, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import AppButtonComponent from '@shared/components/ui/button/button.component';

@Component({
  selector: 'app-rating-buttons',
  imports: [AppButtonComponent, TranslocoPipe],
  templateUrl: './rating-buttons.component.html',
})
export class RatingButtonsComponent {
  readonly rate = output<1 | 3>();
}
