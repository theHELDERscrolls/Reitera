import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LucideCalendar, LucideMail, LucideShield, LucideUser } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

import { AuthService } from '@core/auth/auth.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';

@Component({
  selector: 'app-profile',
  imports: [
    DatePipe,
    PageHeaderComponent,
    TranslocoPipe,
    LucideUser,
    LucideMail,
    LucideShield,
    LucideCalendar,
  ],
  templateUrl: './profile.component.html',
})
export default class ProfileComponent {
  private readonly authService = inject(AuthService);

  readonly currentUser = this.authService.currentUser;

  readonly displayName = computed(() => {
    const user = this.currentUser();

    if (!user) return '';

    return `${user.firstName} ${user.lastName}`.trim();
  });

  readonly initial = computed(() => (this.currentUser()?.username ?? '?').charAt(0).toUpperCase());
}
