import { Component, computed, input } from '@angular/core';

export type CardPadding = 'none' | 'sm' | 'md' | 'lg';
export type CardShadow = 'none' | 'offset';

@Component({
  selector: 'app-card',
  standalone: true,
  imports: [],
  templateUrl: './card.component.html',
})
export default class AppCardComponent {
  readonly padding = input<CardPadding>('md');
  readonly shadow = input<CardShadow>('none');
  readonly interactive = input<boolean>(false);

  private readonly PADDING_CLASSES: Record<CardPadding, string> = {
    none: '',
    sm: 'p-3',
    md: 'p-6',
    lg: 'p-8',
  };

  readonly classes = computed(() => {
    const base = 'rounded-xs border-3 border-border bg-surface transition-all duration-200';
    const padding = this.PADDING_CLASSES[this.padding()];
    const shadow =
      this.shadow() === 'offset' ? 'shadow-[3px_3px_0_0_var(--ctp-mocha-crust)]' : '';
    const interactive = this.interactive()
      ? 'cursor-pointer shadow-[3px_3px_0_0_var(--ctp-mocha-crust)] hover:-translate-y-1 hover:shadow-[6px_6px_0_0_var(--ctp-mocha-crust)] hover:border-border-strong active:translate-y-px active:shadow-[2px_2px_0_0_var(--ctp-mocha-crust)]'
      : '';

    return [base, padding, shadow, interactive].filter(Boolean).join(' ');
  });
}
