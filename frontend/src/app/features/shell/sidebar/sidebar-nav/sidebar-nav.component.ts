import { Component, input, output } from '@angular/core';
import { LucideLayoutDashboard, LucideBookOpen, LucideBrain } from '@lucide/angular';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-sidebar-nav',
  imports: [
    RouterLink,
    RouterLinkActive,
    TranslocoPipe,
    LucideLayoutDashboard,
    LucideBookOpen,
    LucideBrain,
  ],
  templateUrl: './sidebar-nav.component.html',
})
export class SidebarNavComponent {
  readonly isCollapsed = input.required<boolean>();
  readonly navItemClicked = output<void>();
}
