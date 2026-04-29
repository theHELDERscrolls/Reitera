import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { TranslocoPipe } from '@jsverse/transloco';
import {
  LucideBookOpen,
  LucideCalendarCheck,
  LucideFlame,
} from '@lucide/angular';

import { AuthService } from '@core/auth/auth.service';
import { PageHeaderComponent } from '@shared/components/page-header/page-header.component';
import { StatCardComponent } from './components/stat-card/stat-card.component';
import { StudyHeatmapComponent } from './components/study-heatmap/study-heatmap.component';
import { LastStudiedDecksComponent } from './components/last-studied-decks/last-studied-decks.component';
import { DashboardService } from './services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  imports: [
    TranslocoPipe,
    PageHeaderComponent,
    StatCardComponent,
    StudyHeatmapComponent,
    LastStudiedDecksComponent,
    LucideCalendarCheck,
    LucideBookOpen,
    LucideFlame,
  ],
  templateUrl: './dashboard.component.html',
})
export default class DashboardComponent {
  private readonly authService = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);

  readonly firstName = computed(() => this.authService.currentUser()?.firstName ?? '');

  readonly stats = toSignal(
    this.dashboardService.getStats().pipe(catchError(() => of(null))),
    { initialValue: undefined },
  );

  readonly statsLoading = computed(() => this.stats() === undefined);
  readonly statsError = computed(() => this.stats() === null);

  readonly heatmap = toSignal(
    this.dashboardService.getHeatmap().pipe(catchError(() => of(null))),
    { initialValue: undefined },
  );

  readonly heatmapLoading = computed(() => this.heatmap() === undefined);

  readonly lastStudied = toSignal(
    this.dashboardService.getLastStudied().pipe(catchError(() => of([]))),
    { initialValue: undefined },
  );

  readonly lastStudiedLoading = computed(() => this.lastStudied() === undefined);
}
