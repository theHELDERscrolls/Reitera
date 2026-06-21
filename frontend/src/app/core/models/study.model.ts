import { CardType } from './card.model';

export type CardState = 0 | 1 | 2 | 3;

export interface DueCard {
  id: number;
  deckId: number;
  type: CardType;
  question: string;
  answerJson: Record<string, unknown>;
  explanation: string | null;
  state: CardState;
}

export interface CardRating {
  cardId: number;
  rating: 1 | 3;
}

export interface StudySessionRequest {
  deckId: number | null;
  categoryId: number | null;
  ratings: CardRating[];
}

export interface StudySessionResponse {
  deckId: number | null;
  categoryId: number | null;
  cardsReviewed: number;
}

export interface SessionBackup {
  version: '1';
  deckId: number | null;
  deckName: string | null;
  categoryId: number | null;
  categoryName: string | null;
  ratings: CardRating[];
  startedAt: number;
  updatedAt: number;
}
