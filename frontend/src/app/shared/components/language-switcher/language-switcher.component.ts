import { Component, HostListener, inject, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { LucideEarth, LucideChevronDown } from '@lucide/angular';

@Component({
  selector: 'app-language-switcher',
  imports: [LucideEarth, LucideChevronDown],
  templateUrl: './language-switcher.component.html',
  styles: ``,
})
export class LanguageSwitcherComponent {
  private readonly transloco = inject(TranslocoService);

  readonly isOpen = signal(false);

  readonly languages = [
    { code: 'en', label: 'English' },
    { code: 'es', label: 'Español' },
    { code: 'fr', label: 'Français' },
    { code: 'pt', label: 'Português' },
  ];

  get activeLang(): string {
    return this.transloco.getActiveLang();
  }

  get activeLabel(): string {
    return this.languages.find((l) => l.code === this.activeLang)?.label ?? this.activeLang;
  }

  select(code: string): void {
    this.transloco.setActiveLang(code);
    this.isOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('[data-language-switcher]')) {
      this.isOpen.set(false);
    }
  }
}
