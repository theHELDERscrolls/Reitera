import { Component, HostListener, inject, input, output, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

import { AuthService } from '@core/auth/auth.service';
import { ProfilePanelComponent } from './profile-panel/profile-panel.component';
import AppAvatarComponent from '@shared/components/ui/avatar/avatar.component';

@Component({
  selector: 'app-sidebar-footer',
  imports: [TranslocoPipe, ProfilePanelComponent, AppAvatarComponent],
  templateUrl: './sidebar-footer.component.html',
})
export class SidebarFooterComponent {
  private readonly authService = inject(AuthService);

  readonly currentUser = this.authService.currentUser;
  readonly isCollapsed = input.required<boolean>();
  readonly isProfileOpen = signal(false);
  readonly navigated = output<void>();

  toggleProfile(): void {
    this.isProfileOpen.update((v) => !v);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!(event.target as HTMLElement).closest('[data-profile-menu]')) {
      this.isProfileOpen.set(false);
    }
  }
}
