import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { EmailVerificationService } from '../services/email-verification.service';
import { ToastService } from '@core/toast/toast.service';
import { LanguageSwitcherComponent } from '@shared/components/language-switcher/language-switcher.component';
import AppButtonComponent from '@shared/components/ui/button/button.component';

@Component({
  selector: 'app-check-email',
  imports: [RouterLink, TranslocoPipe, LanguageSwitcherComponent, AppButtonComponent],
  templateUrl: './check-email.component.html',
})
export default class CheckEmailComponent {
  private readonly router = inject(Router);
  private readonly emailVerificationService = inject(EmailVerificationService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly email = signal<string>(this.router.currentNavigation()?.extras.state?.['email'] ?? '');

  resend(): void {
    this.emailVerificationService.resendEmail({ email: this.email() }).subscribe({
      complete: () =>
        this.toastService.success(this.transloco.translate('auth.check_email.resend_success')),
      error: () =>
        this.toastService.error(this.transloco.translate('auth.check_email.resend_error')),
    });
  }
}
