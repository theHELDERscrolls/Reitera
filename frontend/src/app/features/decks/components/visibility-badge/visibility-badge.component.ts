import { Component, input } from '@angular/core';
import { LucideGlobe, LucideLock } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-visibility-badge',
  imports: [LucideGlobe, LucideLock, TranslocoPipe],
  templateUrl: './visibility-badge.component.html',
})
export class VisibilityBadgeComponent {
  readonly isPublic = input.required<boolean>();
}
