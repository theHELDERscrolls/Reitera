import { Component, inject, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideSun, LucideMoon, LucideLogOut, LucideUser } from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { AuthService } from '@core/auth/auth.service';
import { ThemeService } from '@core/theme/theme.service';
import { LanguageSwitcherComponent } from "@shared/components/language-switcher/language-switcher.component";

@Component({
  selector: 'app-profile-panel',
  imports: [TranslocoPipe, RouterLink, LucideSun, LucideMoon, LucideLogOut, LucideUser, LanguageSwitcherComponent],
  templateUrl: './profile-panel.component.html',
})
export class ProfilePanelComponent {
  readonly navigated = output<void>();

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
