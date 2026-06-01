import { Component, computed, input } from '@angular/core';

export type BadgeVariant =
  | 'default'
  | 'primary'
  | 'info'
  | 'success'
  | 'warning'
  | 'error'
  | 'accent-1'
  | 'accent-2'
  | 'accent-4'
  | 'accent-5';

export type BadgeSize = 'sm' | 'md';

@Component({
  selector: 'app-badge',
  standalone: true,
  imports: [],
  templateUrl: './badge.component.html',
})
export default class AppBadgeComponent {
  readonly variant = input<BadgeVariant>('default');
  readonly size = input<BadgeSize>('sm');
  readonly dot = input<boolean>(false);

  private readonly VARIANT_CLASSES: Record<BadgeVariant, string> = {
    default: 'bg-overlay text-muted-foreground',
    primary: 'bg-primary/15 text-primary',
    info: 'bg-info/10 text-info',
    success: 'bg-success/10 text-success',
    warning: 'bg-warning/10 text-warning',
    error: 'bg-error/10 text-error',
    'accent-1': 'bg-accent-1/15 text-accent-1',
    'accent-2': 'bg-accent-2/15 text-accent-2',
    'accent-4': 'bg-accent-4/15 text-accent-4',
    'accent-5': 'bg-accent-5/15 text-accent-5',
  };

  private readonly SIZE_CLASSES: Record<BadgeSize, string> = {
    sm: 'px-2 py-0.5 text-xs rounded-full',
    md: 'px-2.5 py-1 text-xs rounded-md font-semibold',
  };

  private readonly DOT_CLASSES: Record<BadgeVariant, string> = {
    default: 'bg-muted-foreground',
    primary: 'bg-primary',
    info: 'bg-info',
    success: 'bg-success',
    warning: 'bg-warning',
    error: 'bg-error',
    'accent-1': 'bg-accent-1',
    'accent-2': 'bg-accent-2',
    'accent-4': 'bg-accent-4',
    'accent-5': 'bg-accent-5',
  };

  readonly classes = computed(() => {
    return [
      'inline-flex items-center gap-1.5 font-medium whitespace-nowrap',
      this.VARIANT_CLASSES[this.variant()],
      this.SIZE_CLASSES[this.size()],
    ].join(' ');
  });

  readonly dotClasses = computed(() => {
    return `size-1.5 rounded-full shrink-0 ${this.DOT_CLASSES[this.variant()]}`;
  });
}
