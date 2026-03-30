import { Component, input, output } from '@angular/core';
import { LucideLibraryBig } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-sidebar-header',
  imports: [TranslocoPipe, LucideLibraryBig],
  templateUrl: './sidebar-header.component.html',
})
export class SidebarHeaderComponent {
  readonly isCollapsed = input.required<boolean>();
  readonly toggle = output<void>();
}
