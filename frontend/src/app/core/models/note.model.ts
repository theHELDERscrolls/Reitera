export type NoteType = 'BASIC' | 'BASIC_REVERSE' | 'CLOZE' | 'MULTIPLE_CHOICE' | 'UNKNOWN';

export interface NoteRequest {
  content: string;
  explanation: string | null;
}

export interface NoteResponse {
  id: number;
  deckId: number;
  type: NoteType;
  content: string;
  explanation: string | null;
  cardCount: number;
  createdAt: string;
}
