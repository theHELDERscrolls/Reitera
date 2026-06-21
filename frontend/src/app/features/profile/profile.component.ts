import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  LucideCalendar,
  LucideExternalLink,
  LucideGitBranch,
  LucideMail,
  LucidePencil,
  LucideX,
} from '@lucide/angular';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { AuthService } from '@core/auth/auth.service';
import { ProfileGuideComponent } from './components/profile-guide/profile-guide.component';
import { ProfileService } from '@features/profile/services/profile.service';
import { ProfileTipsComponent } from './components/profile-tips/profile-tips.component';
import { ToastService } from '@core/toast/toast.service';
import AppButtonComponent from '@shared/components/ui/button/button.component';
import AppInputComponent from '@shared/components/ui/input/input.component';

@Component({
  selector: 'app-profile',
  imports: [
    AppButtonComponent,
    AppInputComponent,
    DatePipe,
    LucideCalendar,
    LucideExternalLink,
    LucideGitBranch,
    LucideMail,
    LucidePencil,
    LucideX,
    ProfileGuideComponent,
    ProfileTipsComponent,
    ReactiveFormsModule,
    TranslocoPipe,
  ],
  templateUrl: './profile.component.html',
})
export default class ProfileComponent {
  private readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);
  private readonly fb = inject(FormBuilder);

  readonly currentUser = this.authService.currentUser;
  readonly isEditing = signal(false);
  readonly isSaving = signal(false);

  readonly avatarIds = [
    'avatar-01',
    'avatar-02',
    'avatar-03',
    'avatar-04',
    'avatar-05',
    'avatar-06',
    'avatar-07',
    'avatar-08',
  ];

  readonly selectedAvatarId = signal<string | null>(null);

  readonly activeAvatarUrl = computed(() => {
    const id = this.currentUser()?.avatarId;
    return id ? `avatars/${id}.svg` : null;
  });

  readonly displayName = computed(() => {
    const user = this.currentUser();

    if (!user) return '';

    return `${user.firstName} ${user.lastName}`.trim();
  });

  readonly initial = computed(() => (this.currentUser()?.username ?? '?').charAt(0).toUpperCase());

  readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(50)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
  });

  readonly firstNameControl = this.form.controls.firstName;
  readonly lastNameControl = this.form.controls.lastName;
  readonly usernameControl = this.form.controls.username;

  startEdit(): void {
    const user = this.currentUser();

    if (!user) return;

    this.form.setValue({
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username,
    });
    this.selectedAvatarId.set(user.avatarId ?? null);
    this.isEditing.set(true);
  }

  cancelEdit(): void {
    this.form.reset();
    this.isEditing.set(false);
  }

  save(): void {
    if (this.form.invalid || this.isSaving()) return;

    const { firstName, lastName, username } = this.form.getRawValue();

    this.isSaving.set(true);

    this.profileService
      .updateMe({
        firstName: firstName!,
        lastName: lastName!,
        username: username!,
        avatarId: this.selectedAvatarId(),
      })
      .subscribe({
        next: () => {
          this.isSaving.set(false);
          this.isEditing.set(false);
          this.authService.refreshCurrentUser();
          this.toastService.success(this.transloco.translate('profile.saveSuccess'));
        },
        error: (err) => {
          this.isSaving.set(false);

          const is409 = err?.status === 409;

          this.toastService.error(
            this.transloco.translate(
              is409 ? 'profile.form.error.usernameTaken' : 'profile.form.error.saveFailed',
            ),
          );
        },
      });
  }
}
