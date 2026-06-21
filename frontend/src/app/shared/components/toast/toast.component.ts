import { Component, inject } from '@angular/core';
import { LucideX } from '@lucide/angular';

import { ToastService } from '@core/toast/toast.service';
import { Toast, ToastType } from '@core/models/toast.model';

@Component({
  selector: 'app-toast',
  imports: [LucideX],
  templateUrl: './toast.component.html',
  styles: ``,
})
export class ToastComponent {
  readonly toastService = inject(ToastService);

  trackById(_: number, toast: Toast): number {
    return toast.id;
  }

  typeClasses(type: ToastType): string {
    const map: Record<ToastType, string> = {
      success: 'bg-success border-primary-foreground/50',
      error: 'bg-error border-primary-foreground/50',
      warning: 'bg-warning border-primary-foreground/50',
      info: 'bg-info border-primary-foreground/50',
    };

    return map[type];
  }
}
