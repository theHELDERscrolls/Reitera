import { Component, inject } from '@angular/core';
import { LucideSun, LucideMoon, LucideLogOut } from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { AuthService } from '@core/auth/auth.service';
import { ThemeService } from '@core/theme/theme.service';

@Component({
  selector: 'app-profile-panel',
  imports: [TranslocoPipe, LucideSun, LucideMoon, LucideLogOut],
  templateUrl: './profile-panel.component.html',
})
export class ProfilePanelComponent {
  private readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);
  private readonly transloco = inject(TranslocoService);

  readonly currentUser = this.authService.currentUser;

  readonly languages = [
    { code: 'en', label: 'English' },
    { code: 'es', label: 'Español' },
    { code: 'fr', label: 'Français' },
  ];

  get activeLang(): string {
    return this.transloco.getActiveLang();
  }

  get isDark(): boolean {
    return this.themeService.theme() === 'dark';
  }

  selectLang(code: string): void {
    this.transloco.setActiveLang(code);
  }

  logout(): void {
    this.authService.logout();
  }
}
