import { Component, computed, input, output } from '@angular/core';
import { LucideCircleCheck, LucideInfo, LucideTriangleAlert } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

export type DialogVariant = 'danger' | 'warning' | 'success' | 'info';

interface VariantConfig {
  iconBg: string;
  iconColor: string;
  buttonClass: string;
}

const VARIANT_CONFIG: Record<DialogVariant, VariantConfig> = {
  danger: {
    iconBg: 'bg-error/10',
    iconColor: 'text-error',
    buttonClass: 'bg-error hover:bg-error/90 text-error-foreground',
  },
  warning: {
    iconBg: 'bg-warning/10',
    iconColor: 'text-warning',
    buttonClass: 'bg-warning hover:bg-warning/90 text-warning-foreground',
  },
  success: {
    iconBg: 'bg-success/10',
    iconColor: 'text-success',
    buttonClass: 'bg-success hover:bg-success/90 text-success-foreground',
  },
  info: {
    iconBg: 'bg-info/10',
    iconColor: 'text-info',
    buttonClass: 'bg-info hover:bg-info/90 text-info-foreground',
  },
};

@Component({
  selector: 'app-confirm-dialog',
  imports: [TranslocoPipe, LucideTriangleAlert, LucideCircleCheck, LucideInfo],
  templateUrl: './confirm-dialog.component.html',
})
export class ConfirmDialogComponent {
  readonly confirmLabel = input<string | null>(null);
  readonly message = input.required<string>();
  readonly title = input.required<string>();
  readonly variant = input<DialogVariant>('danger');

  readonly cancelled = output<void>();
  readonly confirmed = output<void>();

  readonly config = computed(() => VARIANT_CONFIG[this.variant()]);
}
