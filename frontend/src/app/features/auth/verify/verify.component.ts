import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { LucideLoaderCircle, LucideMail } from '@lucide/angular';

import { EmailVerificationService } from '../services/email-verification.service';
import { ToastService } from '@core/toast/toast.service';
import { LanguageSwitcherComponent } from '@shared/components/language-switcher/language-switcher.component';
import AppButtonComponent from '@shared/components/ui/button/button.component';
import AppInputComponent from '@shared/components/ui/input/input.component';

@Component({
  selector: 'app-verify',
  imports: [ReactiveFormsModule, RouterLink, TranslocoPipe, LanguageSwitcherComponent, AppButtonComponent, AppInputComponent, LucideLoaderCircle],
  templateUrl: './verify.component.html',
})
export default class VerifyComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly emailVerificationService = inject(EmailVerificationService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly mailIcon = LucideMail;

  readonly isLoading = signal(true);
  readonly hasError = signal(false);

  readonly emailControl = new FormControl('', {
    validators: [Validators.required, Validators.email],
    nonNullable: true,
  });

  ngOnInit(): void {
    const token = this.route.snapshot.queryParams['token'] as string | undefined;

    if (!token) {
      this.isLoading.set(false);
      this.hasError.set(true);
      this.toastService.error(this.transloco.translate('auth.verify.error'));
      return;
    }

    this.emailVerificationService.verifyEmail(token).subscribe({
      complete: () => {
        this.toastService.success(this.transloco.translate('auth.verify.success'));
        void this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.isLoading.set(false);
        this.hasError.set(true);
        this.toastService.error(this.transloco.translate('auth.verify.error'));
      },
    });
  }

  resend(): void {
    if (this.emailControl.invalid) {
      this.emailControl.markAsTouched();
      return;
    }

    this.emailVerificationService.resendEmail({ email: this.emailControl.value }).subscribe({
      complete: () =>
        this.toastService.success(this.transloco.translate('auth.verify.resend_success')),
      error: () => this.toastService.error(this.transloco.translate('auth.verify.resend_error')),
    });
  }
}
