import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { NoteRequest, NoteResponse } from '@core/models/note.model';
import { Page } from '@core/models/page.model';
import { environment } from '@environments/environment';

@Injectable({ providedIn: 'root' })
export class NoteService {
  private readonly http = inject(HttpClient);
  private readonly decksUrl = `${environment.apiUrl}/decks`;

  createNote(deckId: number, dto: NoteRequest): Observable<NoteResponse> {
    return this.http.post<NoteResponse>(`${this.decksUrl}/${deckId}/notes`, dto);
  }

  getNotesByDeck(
    deckId: number,
    page = 0,
    size = 20,
    sort = 'createdAt',
    direction: 'asc' | 'desc' = 'desc',
  ): Observable<Page<NoteResponse>> {
    return this.http.get<Page<NoteResponse>>(`${this.decksUrl}/${deckId}/notes`, {
      params: { page, size, sort: `${sort},${direction}` },
    });
  }

  getNoteById(deckId: number, noteId: number): Observable<NoteResponse> {
    return this.http.get<NoteResponse>(`${this.decksUrl}/${deckId}/notes/${noteId}`);
  }

  updateNote(deckId: number, noteId: number, dto: NoteRequest): Observable<NoteResponse> {
    return this.http.put<NoteResponse>(`${this.decksUrl}/${deckId}/notes/${noteId}`, dto);
  }

  deleteNote(deckId: number, noteId: number): Observable<void> {
    return this.http.delete<void>(`${this.decksUrl}/${deckId}/notes/${noteId}`);
  }
}
