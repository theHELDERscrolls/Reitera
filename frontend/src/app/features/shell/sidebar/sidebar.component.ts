import { Component, signal } from '@angular/core';
import { LucideX } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

import { SidebarFooterComponent } from './sidebar-footer/sidebar-footer.component';
import { SidebarHeaderComponent } from './sidebar-header/sidebar-header.component';
import { SidebarNavComponent } from './sidebar-nav/sidebar-nav.component';

const SIDEBAR_KEY = 'sidebar_collapsed';

@Component({
  selector: 'app-sidebar',
  imports: [
    LucideX,
    SidebarFooterComponent,
    SidebarHeaderComponent,
    SidebarNavComponent,
    TranslocoPipe,
  ],
  templateUrl: './sidebar.component.html',
})
export class SidebarComponent {
  readonly isCollapsed = signal<boolean>(localStorage.getItem(SIDEBAR_KEY) === 'true');
  readonly isMobileOpen = signal(false);

  toggleCollapse(): void {
    this.isCollapsed.update((v) => {
      const next = !v;
      localStorage.setItem(SIDEBAR_KEY, String(next));
      return next;
    });
  }

  toggleMobile(): void {
    this.isMobileOpen.update((v) => !v);
  }
}
