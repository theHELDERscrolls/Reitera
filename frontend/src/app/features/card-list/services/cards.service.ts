import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CardResponse } from '@core/models/card.model';
import { environment } from '@environments/environment';
import { Page } from '@core/models/page.model';

export interface CardFilters {
  question?: string;
  type?: string | null;
  state?: number | null;
}

@Injectable({ providedIn: 'root' })
export class CardsService {
  private readonly http = inject(HttpClient);
  private readonly cardsUrl = `${environment.apiUrl}/cards`;

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

    return this.http.get<Page<CardResponse>>(this.cardsUrl, { params });
  }
}
