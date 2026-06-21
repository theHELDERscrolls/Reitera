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
    buttonClass:
      'border-3 border-error bg-error text-error-foreground rounded-xs shadow-[6px_6px_0_0_var(--ctp-mocha-crust)] hover:translate-x-0.75 hover:translate-y-0.75 hover:shadow-[3px_3px_0_0_var(--ctp-mocha-crust)] active:translate-x-1.5 active:translate-y-1.5 active:shadow-none',
  },
  warning: {
    iconBg: 'bg-warning/10',
    iconColor: 'text-warning',
    buttonClass:
      'border-3 border-warning bg-warning text-warning-foreground rounded-xs shadow-[6px_6px_0_0_var(--ctp-mocha-crust)] hover:translate-x-0.75 hover:translate-y-0.75 hover:shadow-[3px_3px_0_0_var(--ctp-mocha-crust)] active:translate-x-1.5 active:translate-y-1.5 active:shadow-none',
  },
  success: {
    iconBg: 'bg-success/10',
    iconColor: 'text-success',
    buttonClass:
      'border-3 border-success bg-success text-success-foreground rounded-xs shadow-[6px_6px_0_0_var(--ctp-mocha-crust)] hover:translate-x-0.75 hover:translate-y-0.75 hover:shadow-[3px_3px_0_0_var(--ctp-mocha-crust)] active:translate-x-1.5 active:translate-y-1.5 active:shadow-none',
  },
  info: {
    iconBg: 'bg-info/10',
    iconColor: 'text-info',
    buttonClass:
      'border-3 border-info bg-info text-info-foreground rounded-xs shadow-[6px_6px_0_0_var(--ctp-mocha-crust)] hover:translate-x-0.75 hover:translate-y-0.75 hover:shadow-[3px_3px_0_0_var(--ctp-mocha-crust)] active:translate-x-1.5 active:translate-y-1.5 active:shadow-none',
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
