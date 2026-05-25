import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CardRequest, CardResponse } from '@core/models/card.model';
import { DeckResponse, DeckStats } from '@core/models/deck.model';
import { environment } from '@environments/environment';
import { Page } from '@core/models/page.model';

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

  getCards(
    deckId: number,
    page = 0,
    size = 20,
    sort = 'question',
    direction: 'asc' | 'desc' = 'asc',
  ): Observable<Page<CardResponse>> {
    return this.http.get<Page<CardResponse>>(`${this.decksUrl}/${deckId}/cards`, {
      params: { page, size, sort: `${sort},${direction}` },
    });
  }

  createCard(deckId: number, dto: CardRequest): Observable<CardResponse> {
    return this.http.post<CardResponse>(`${this.decksUrl}/${deckId}/cards`, dto);
  }

  updateCard(deckId: number, cardId: number, dto: CardRequest): Observable<CardResponse> {
    return this.http.put<CardResponse>(`${this.decksUrl}/${deckId}/cards/${cardId}`, dto);
  }

  deleteCard(deckId: number, cardId: number): Observable<void> {
    return this.http.delete<void>(`${this.decksUrl}/${deckId}/cards/${cardId}`);
  }
}
