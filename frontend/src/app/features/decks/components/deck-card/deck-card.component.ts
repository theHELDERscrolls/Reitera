import { Component, HostListener, computed, input, output, signal } from '@angular/core';
import {
  LucideCalendar,
  LucideEdit,
  LucideEllipsisVertical,
  LucideGlobe,
  LucideLock,
  LucideTrash2,
} from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

import { DeckResponse } from '@core/models/deck.model';
import { DatePipe } from '@angular/common';

const ACCENT_COLORS = [
  'var(--color-accent-1)',
  'var(--color-accent-2)',
  'var(--color-accent-3)',
  'var(--color-accent-4)',
  'var(--color-accent-5)',
  'var(--color-accent-6)',
] as const;

@Component({
  selector: 'app-deck-card',
  imports: [
    DatePipe,
    LucideCalendar,
    LucideEdit,
    LucideEllipsisVertical,
    LucideGlobe,
    LucideLock,
    LucideTrash2,
    TranslocoPipe,
  ],
  templateUrl: './deck-card.component.html',
})
export class DeckCardComponent {
  readonly deck = input.required<DeckResponse>();
  readonly edit = output<DeckResponse>();
  readonly delete = output<DeckResponse>();
  readonly isMenuOpen = signal(false);
  readonly accentColor = computed(() => ACCENT_COLORS[this.deck().id % ACCENT_COLORS.length]);

  toggleMenu(): void {
    this.isMenuOpen.update((v) => !v);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!(event.target as HTMLElement).closest('[data-deck-menu]')) {
      this.isMenuOpen.set(false);
    }
  }
}
