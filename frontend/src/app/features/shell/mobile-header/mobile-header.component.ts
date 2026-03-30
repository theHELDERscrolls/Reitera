import { Component, output } from '@angular/core';
import { LucideMenu } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-mobile-header',
  imports: [TranslocoPipe, LucideMenu],
  templateUrl: './mobile-header.component.html',
})
export class MobileHeaderComponent {
  readonly openMenu = output<void>();
}
