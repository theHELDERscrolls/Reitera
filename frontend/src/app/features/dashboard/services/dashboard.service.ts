import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@environments/environment';
import { DailyStudyCount, DashboardStats, LastStudiedDeck } from '@core/models/dashboard.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/dashboard`;

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/stats`);
  }

  getHeatmap(): Observable<DailyStudyCount[]> {
    return this.http.get<DailyStudyCount[]>(`${this.baseUrl}/heatmap`);
  }

  getLastStudied(limit = 5): Observable<LastStudiedDeck[]> {
    return this.http.get<LastStudiedDeck[]>(`${this.baseUrl}/last-studied`, {
      params: { limit },
    });
  }
}
