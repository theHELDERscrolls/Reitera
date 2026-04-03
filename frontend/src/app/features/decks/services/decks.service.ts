import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@environments/environment';
import { DeckRequest, DeckResponse } from '@core/models/deck.model';
import { Category } from '@core/models/category.model';
import { Page } from '@core/models/page.model';

@Injectable({
  providedIn: 'root',
})
export class DecksService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/decks`;
  private readonly categoriesUrl = `${environment.apiUrl}/categories`;

  getDecks(page = 0, size = 12): Observable<Page<DeckResponse>> {
    return this.http.get<Page<DeckResponse>>(this.baseUrl, {
      params: { page, size, sort: 'createdAt,desc' },
    });
  }

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.categoriesUrl);
  }

  createDeck(deck: DeckRequest): Observable<DeckResponse> {
    return this.http.post<DeckResponse>(this.baseUrl, deck);
  }

  updateDeck(id: number, deck: DeckRequest): Observable<DeckResponse> {
    return this.http.put<DeckResponse>(`${this.baseUrl}/${id}`, deck);
  }

  deleteDeck(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
