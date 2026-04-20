import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CardRequest, CardResponse } from '@core/models/card.model';
import { environment } from '@environments/environment';
import { Page } from '@core/models/page.model';
import { Tag } from '@core/models/tag.model';

export interface CardFilters {
  question?: string;
  type?: string | null;
  state?: number | null;
  tagId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class CardsService {
  private readonly http = inject(HttpClient);
  private readonly cardsUrl = `${environment.apiUrl}/cards`;
  private readonly decksUrl = `${environment.apiUrl}/decks`;
  private readonly tagsUrl = `${environment.apiUrl}/tags`;

  /**
   * Fetches a paginated, filtered list of all cards owned by the current user.
   * Only non-null, non-empty filters are sent as query parameters.
   */
  getCards(
    filters: CardFilters,
    page = 0,
    size = 20,
    sort = 'question',
    direction: 'asc' | 'desc' = 'asc',
  ): Observable<Page<CardResponse>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', `${sort},${direction}`);

    if (filters.question?.trim()) {
      params = params.set('question', filters.question.trim());
    }
    
    if (filters.type) {
      params = params.set('type', filters.type);
    }
    
    if (filters.state !== null && filters.state !== undefined) {
      params = params.set('state', filters.state);
    }
    
    if (filters.tagId !== null && filters.tagId !== undefined) {
      params = params.set('tagId', filters.tagId);
    }

    return this.http.get<Page<CardResponse>>(this.cardsUrl, { params });
  }

  /**
   * Fetches all tags owned by the current user.
   * Used to populate the tag filter dropdown.
   */
  getTags(): Observable<Tag[]> {
    return this.http.get<Tag[]>(this.tagsUrl);
  }

  /**
   * Updates an existing card. Requires the deckId because the edit endpoint
   * is nested under the deck: PUT /api/v1/decks/{deckId}/cards/{cardId}.
   * The deckId is always available from the CardResponse object.
   */
  updateCard(deckId: number, cardId: number, dto: CardRequest): Observable<CardResponse> {
    return this.http.put<CardResponse>(`${this.decksUrl}/${deckId}/cards/${cardId}`, dto);
  }

  /**
   * Deletes a card. Same reason as updateCard — uses the deck-nested endpoint.
   */
  deleteCard(deckId: number, cardId: number): Observable<void> {
    return this.http.delete<void>(`${this.decksUrl}/${deckId}/cards/${cardId}`);
  }
}
