import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { AuthService } from '@core/auth/auth.service';
import { LanguageSwitcherComponent } from '@shared/components/language-switcher/language-switcher.component';
import { RegisterRequest } from '@core/models/auth.model';
import { ToastService } from '@core/toast/toast.service';

const passwordPattern = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$/;

const matchPasswords: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;

  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, LanguageSwitcherComponent],
  templateUrl: './register.component.html',
  styles: ``,
})
export default class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly transloco = inject(TranslocoService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(false);

  readonly form = this.fb.group(
    {
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      firstName: ['', [Validators.required, Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      password: [
        '',
        [Validators.required, Validators.maxLength(128), Validators.pattern(passwordPattern)],
      ],
      confirmPassword: ['', [Validators.required, Validators.maxLength(128)]],
    },
    { validators: matchPasswords },
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);

    this.authService.register(this.form.value as RegisterRequest).subscribe({
      next: () => {
        this.toast.success(this.transloco.translate('auth.register.success'));
        void this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.toast.error(
          err.error?.message ?? this.transloco.translate('auth.register.error.server'),
        );
        this.loading.set(false);
      },
    });
  }
}
