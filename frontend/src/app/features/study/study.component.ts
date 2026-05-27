import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { SessionBackupService } from '@features/study/services/session-backup.service';
import { StudyHubComponent } from './components/study-hub/study-hub.component';
import { StudySessionComponent } from './components/study-session/study-session.component';

@Component({
  selector: 'app-study',
  imports: [ConfirmDialogComponent, StudyHubComponent, StudySessionComponent, TranslocoPipe],
  templateUrl: './study.component.html',
})
export default class StudyComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sessionBackupService = inject(SessionBackupService);
  private readonly transloco = inject(TranslocoService);

  private readonly queryParams = toSignal(this.route.queryParamMap, { requireSync: true });

  readonly deckId = computed(() => {
    const val = this.queryParams().get('deckId');
    return val ? Number(val) : null;
  });

  readonly categoryId = computed(() => {
    const val = this.queryParams().get('categoryId');
    return val ? Number(val) : null;
  });

  readonly deckName = computed(() => this.queryParams().get('deckName'));
  readonly categoryName = computed(() => this.queryParams().get('categoryName'));

  readonly mode = computed(() =>
    this.deckId() !== null || this.categoryId() !== null ? 'session' : 'hub',
  );

  // Conflicto: hay backup de OTRO mazo y el usuario acaba de entrar en una sesión diferente
  readonly conflictBackup = computed(() => {
    if (this.mode() !== 'session') return null;
    const backup = this.sessionBackupService.backup();
    if (!backup) return null;
    const sameSession = backup.deckId === this.deckId() && backup.categoryId === this.categoryId();
    return sameSession ? null : backup;
  });

  readonly conflictMessage = computed(() => {
    const backup = this.conflictBackup();
    if (!backup) return '';
    const name = backup.deckName ?? backup.categoryName ?? '';
    return this.transloco.translate('study.conflict.message', { name });
  });

  confirmConflict(): void {
    this.sessionBackupService.clear();
  }

  cancelConflict(): void {
    this.router.navigate(['/study']);
  }
}
