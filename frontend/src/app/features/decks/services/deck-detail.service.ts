import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DeckResponse, DeckStats } from '@core/models/deck.model';
import { environment } from '@environments/environment';

@Injectable({ providedIn: 'root' })
export class DeckDetailService {
  private readonly http = inject(HttpClient);
  private readonly decksUrl = `${environment.apiUrl}/decks`;

  getDeck(id: number): Observable<DeckResponse> {
    return this.http.get<DeckResponse>(`${this.decksUrl}/${id}`);
  }

  getDeckStats(id: number): Observable<DeckStats> {
    return this.http.get<DeckStats>(`${this.decksUrl}/${id}/stats`);
  }
}
