import { Component, computed, input, output } from '@angular/core';
import { LucideDynamicIcon, LucideIconInput, LucideLoaderCircle } from '@lucide/angular';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'success';
export type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [LucideDynamicIcon, LucideLoaderCircle],
  templateUrl: './button.component.html',
})
export default class AppButtonComponent {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly disabled = input<boolean>(false);
  readonly loading = input<boolean>(false);
  readonly fullWidth = input<boolean>(false);
  readonly iconLeft = input<LucideIconInput | null>(null);
  readonly iconRight = input<LucideIconInput | null>(null);

  readonly clicked = output<MouseEvent>();

  private readonly BASE =
    'inline-flex items-center justify-center gap-2 font-semibold transition-all ' +
    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 ' +
    'disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer select-none';

  private readonly VARIANT_CLASSES: Record<ButtonVariant, string> = {
    primary: 'bg-primary text-primary-foreground border-3 border-primary rounded-xs',
    secondary: 'bg-surface text-foreground border-3 border-border rounded-xs',
    ghost:
      'bg-transparent text-muted-foreground border-3 border-border rounded-xs ' +
      'hover:text-foreground hover:bg-overlay',
    danger: 'bg-error text-error-foreground border-3 border-error rounded-xs',
    success: 'bg-success text-success-foreground border-3 border-success rounded-xs',
  };

  private readonly SIZE_CLASSES: Record<ButtonSize, string> = {
    sm:
      'px-3 py-1.5 text-xs shadow-[4px_4px_0_0_var(--ctp-mocha-crust)] ' +
      'hover:translate-x-0.5 hover:translate-y-0.5 hover:shadow-[2px_2px_0_0_var(--ctp-mocha-crust)] ' +
      'active:translate-x-1 active:translate-y-1 active:shadow-none',
    md:
      'px-4 py-2.5 text-sm shadow-[6px_6px_0_0_var(--ctp-mocha-crust)] ' +
      'hover:translate-x-0.75 hover:translate-y-0.75 hover:shadow-[3px_3px_0_0_var(--ctp-mocha-crust)] ' +
      'active:translate-x-1.5 active:translate-y-1.5 active:shadow-none',
    lg:
      'px-5 py-3 text-base shadow-[8px_8px_0_0_var(--ctp-mocha-crust)] ' +
      'hover:translate-x-1 hover:translate-y-1 hover:shadow-[4px_4px_0_0_var(--ctp-mocha-crust)] ' +
      'active:translate-x-2 active:translate-y-2 active:shadow-none',
  };

  private readonly ICON_SIZE_CLASSES: Record<ButtonSize, string> = {
    sm: 'size-3.5',
    md: 'size-4',
    lg: 'size-5',
  };

  readonly classes = computed(() => {
    return [
      this.BASE,
      this.VARIANT_CLASSES[this.variant()],
      this.SIZE_CLASSES[this.size()],
      this.fullWidth() ? 'w-full' : '',
    ]
      .filter(Boolean)
      .join(' ');
  });

  readonly iconSizeClass = computed(() => this.ICON_SIZE_CLASSES[this.size()]);

  readonly isDisabled = computed(() => this.disabled() || this.loading());
}
