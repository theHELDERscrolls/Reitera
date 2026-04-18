import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@environments/environment';
import { DueCard, StudySessionRequest, StudySessionResponse } from '@core/models/study.model';

@Injectable({
  providedIn: 'root',
})
export class StudyService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/study`;

  getDueCards(deckId?: number, categoryId?: number): Observable<DueCard[]> {
    const params: Record<string, number> = {};

    if (deckId !== undefined) params['deckId'] = deckId;

    if (categoryId !== undefined) params['categoryId'] = categoryId;

    return this.http.get<DueCard[]>(`${this.baseUrl}/due`, { params });
  }

  processSession(request: StudySessionRequest): Observable<StudySessionResponse> {
    return this.http.post<StudySessionResponse>(`${this.baseUrl}/sessions`, request);
  }
}
