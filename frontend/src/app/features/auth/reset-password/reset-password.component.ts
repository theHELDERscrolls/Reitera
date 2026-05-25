import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { PasswordResetService } from '../services/password-reset.service';
import { ToastService } from '@core/toast/toast.service';
import { LanguageSwitcherComponent } from '@shared/components/language-switcher/language-switcher.component';

const passwordPattern = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$/;

const matchPasswords: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return password === confirm ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, LanguageSwitcherComponent],
  templateUrl: './reset-password.component.html',
})
export default class ResetPasswordComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly passwordResetService = inject(PasswordResetService);
  private readonly toast = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  private token: string | null = null;

  readonly loading = signal(false);
  readonly tokenState = signal<'valid' | 'missing' | 'expired' | 'invalid'>('valid');

  readonly form = this.fb.group(
    {
      newPassword: [
        '',
        [Validators.required, Validators.maxLength(128), Validators.pattern(passwordPattern)],
      ],
      confirmPassword: ['', [Validators.required, Validators.maxLength(128)]],
    },
    { validators: matchPasswords },
  );

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParams['token'] as string | null;
    if (!this.token) {
      this.tokenState.set('missing');
    }
  }

  submit(): void {
    if (!this.token) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      
      return;
    }

    this.loading.set(true);

    this.passwordResetService
      .resetPassword({ token: this.token, newPassword: this.form.value.newPassword! })
      .subscribe({
        complete: () => {
          this.toast.success(this.transloco.translate('auth.reset_password.success'));
          void this.router.navigate(['/auth/login']);
        },
        error: (err: { status: number }) => {
          this.loading.set(false);
          if (err.status === 410) {
            this.tokenState.set('expired');
          } else if (err.status === 400) {
            this.tokenState.set('invalid');
          } else {
            this.toast.error(this.transloco.translate('auth.reset_password.error.server'));
          }
        },
      });
  }
}
