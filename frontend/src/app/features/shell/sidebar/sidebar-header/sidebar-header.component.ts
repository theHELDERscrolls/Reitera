import { Component, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-sidebar-header',
  imports: [TranslocoPipe],
  templateUrl: './sidebar-header.component.html',
})
export class SidebarHeaderComponent {
  readonly isCollapsed = input.required<boolean>();
  readonly toggled = output<void>();
}
