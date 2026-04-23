import { Component, input, output } from '@angular/core';
import { LucideChartBar, LucideBrain, LucideWalletCards, LucideSquareStack } from '@lucide/angular';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-sidebar-nav',
  imports: [
    LucideBrain,
    LucideChartBar,
    LucideSquareStack,
    LucideWalletCards,
    RouterLink,
    RouterLinkActive,
    TranslocoPipe,
  ],
  templateUrl: './sidebar-nav.component.html',
})
export class SidebarNavComponent {
  readonly isCollapsed = input.required<boolean>();
  readonly navItemClicked = output<void>();
}
