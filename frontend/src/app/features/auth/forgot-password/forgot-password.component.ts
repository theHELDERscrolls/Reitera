import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

import { PasswordResetService } from '../services/password-reset.service';
import { LanguageSwitcherComponent } from '@shared/components/language-switcher/language-switcher.component';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, LanguageSwitcherComponent],
  templateUrl: './forgot-password.component.html',
})
export default class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly passwordResetService = inject(PasswordResetService);

  readonly loading = signal(false);
  readonly submitted = signal(false);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);

    this.passwordResetService.forgotPassword({ email: this.form.value.email! }).subscribe({
      complete: () => {
        this.submitted.set(true);
        this.loading.set(false);
      },
      error: () => {
        this.submitted.set(true);
        this.loading.set(false);
      },
    });
  }
}
