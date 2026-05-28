import { Component, input, output } from '@angular/core';
import { LucideHistory } from '@lucide/angular';

@Component({
  selector: 'app-session-recovery-notice',
  imports: [LucideHistory],
  templateUrl: './session-recovery-notice.component.html',
})
export class SessionRecoveryNoticeComponent {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly resumeLabel = input.required<string>();
  readonly discardLabel = input.required<string>();

  readonly resumed = output<void>();
  readonly discarded = output<void>();
}
