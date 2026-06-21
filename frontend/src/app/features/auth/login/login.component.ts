import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { LucideLock, LucideMail } from '@lucide/angular';

import { AuthService } from '@core/auth/auth.service';
import { LoginRequest } from '@core/models/auth.model';
import { ToastService } from '@core/toast/toast.service';
import { LanguageSwitcherComponent } from '@shared/components/language-switcher/language-switcher.component';
import AppButtonComponent from '@shared/components/ui/button/button.component';
import AppInputComponent from '@shared/components/ui/input/input.component';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    TranslocoPipe,
    LanguageSwitcherComponent,
    AppButtonComponent,
    AppInputComponent,
  ],
  templateUrl: './login.component.html',
})
export default class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly transloco = inject(TranslocoService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);
  readonly mailIcon = LucideMail;
  readonly lockIcon = LucideLock;

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);

    this.authService.login(this.form.value as LoginRequest).subscribe({
      next: () => {
        this.toast.success(this.transloco.translate('auth.login.success'));
        void this.router.navigate(['/decks']);
      },
      error: (err) => {
        this.toast.error(err.error?.message ?? this.transloco.translate('auth.login.error.server'));
        this.loading.set(false);
      },
    });
  }
}
