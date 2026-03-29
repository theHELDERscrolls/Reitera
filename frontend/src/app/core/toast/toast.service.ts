import { Injectable, signal } from '@angular/core';

import { Toast, ToastType } from '@core/models/toast.model';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<Toast[]>([]);
  private nextId = 0;

  readonly toasts = this._toasts.asReadonly();

  show(message: string, type: ToastType, duration = 4000): void {
    const id = this.nextId++;
    this._toasts.update((current) => [...current, { id, message, type, duration }]);
    setTimeout(() => this.dismiss(id), duration);
  }

  success(message: string, duration?: number): void {
    this.show(message, 'success', duration);
  }

  error(message: string, duration?: number): void {
    this.show(message, 'error', duration);
  }

  warning(message: string, duration?: number): void {
    this.show(message, 'warning', duration);
  }

  info(message: string, duration?: number): void {
    this.show(message, 'info', duration);
  }

  dismiss(id: number): void {
    this._toasts.update((current) => current.filter((t) => t.id !== id));
  }
}
